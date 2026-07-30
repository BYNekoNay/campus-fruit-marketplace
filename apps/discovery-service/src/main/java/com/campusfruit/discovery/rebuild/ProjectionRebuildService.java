package com.campusfruit.discovery.rebuild;

import com.campusfruit.discovery.client.MerchantServiceClient;
import com.campusfruit.discovery.client.OfferServiceClient;
import com.campusfruit.discovery.dto.RebuildStatusDTO;
import com.campusfruit.discovery.entity.ProjectionChangeLog;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.fallback.DiscoveryFallbackConfig;
import com.campusfruit.discovery.repository.ProjectionChangeLogRepository;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ProjectionRebuildService {

    private static final Logger log = LoggerFactory.getLogger(ProjectionRebuildService.class);
    private static final String SHADOW_TABLE = "store_offer_projections_shadow";
    private static final String MAIN_TABLE = "store_offer_projections";
    private static final String OLD_TABLE = "store_offer_projections_old";

    private final StoreOfferProjectionRepository projectionRepository;
    private final ProjectionChangeLogRepository changeLogRepository;
    private final MerchantServiceClient merchantServiceClient;
    private final OfferServiceClient offerServiceClient;
    private final DiscoveryFallbackConfig fallbackConfig;
    private final JdbcTemplate jdbcTemplate;

    private final AtomicReference<RebuildStatusDTO> currentStatus = new AtomicReference<>(RebuildStatusDTO.pending());

    public ProjectionRebuildService(StoreOfferProjectionRepository projectionRepository,
                                     ProjectionChangeLogRepository changeLogRepository,
                                     MerchantServiceClient merchantServiceClient,
                                     OfferServiceClient offerServiceClient,
                                     DiscoveryFallbackConfig fallbackConfig,
                                     JdbcTemplate jdbcTemplate) {
        this.projectionRepository = projectionRepository;
        this.changeLogRepository = changeLogRepository;
        this.merchantServiceClient = merchantServiceClient;
        this.offerServiceClient = offerServiceClient;
        this.fallbackConfig = fallbackConfig;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 重建所有源服务的投影数据。
     */
    @Async
    public void rebuildAll() {
        try {
            rebuildFromSource("merchant-service");
            rebuildFromSource("offer-service");
        } catch (Exception e) {
            log.error("rebuildAll failed", e);
            currentStatus.set(RebuildStatusDTO.failed("rebuildAll failed: " + e.getMessage()));
        }
    }

    /**
     * 重建指定源服务的投影数据。
     *
     * @param sourceService 源服务名：merchant-service 或 offer-service
     */
    @Async
    @Transactional
    public void rebuildFromSource(String sourceService) {
        log.info("Starting projection rebuild for sourceService={}", sourceService);
        currentStatus.set(RebuildStatusDTO.inProgress(sourceService));

        try {
            // Step 1: 从源服务获取全量快照
            long snapshotStartSeq = recordSnapshotToken(sourceService);
            log.info("Snapshot token recorded: sourceService={}, sequence={}", sourceService, snapshotStartSeq);

            // Step 2: 写入影子表
            dropShadowTable();
            createShadowTable();

            List<JsonNode> projections;
            long recordsWritten = 0;
            if ("merchant-service".equals(sourceService)) {
                projections = merchantServiceClient.fetchProjections();
                recordsWritten = writeMerchantToShadow(projections);
            } else if ("offer-service".equals(sourceService)) {
                projections = offerServiceClient.fetchProjections();
                recordsWritten = writeOfferToShadow(projections);
            } else {
                throw new IllegalArgumentException("Unknown sourceService: " + sourceService);
            }

            RebuildStatusDTO status = currentStatus.get();
            status.setRecordsWritten(recordsWritten);
            status.setSnapshotToken(String.valueOf(snapshotStartSeq));
            currentStatus.set(status);

            log.info("Shadow table populated: sourceService={}, records={}", sourceService, recordsWritten);

            // Step 3: 拉取增量 delta
            long highWatermark = pullDelta(sourceService, snapshotStartSeq);
            status = currentStatus.get();
            status.setHighWatermark(highWatermark);

            // Step 4: 验证序列无空洞
            boolean hasGap = checkSequenceGaps(sourceService);
            status.setSequenceGapDetected(hasGap);
            if (hasGap) {
                log.warn("Sequence gaps detected for sourceService={}", sourceService);
            }
            currentStatus.set(status);

            // Step 5-7: 原子切换 + 标记数据可用
            atomicSwap();
            fallbackConfig.markDataAvailable();

            log.info("Projection rebuild completed for sourceService={}", sourceService);
            currentStatus.set(RebuildStatusDTO.completed(sourceService, recordsWritten,
                    highWatermark - snapshotStartSeq));
        } catch (Exception e) {
            log.error("Projection rebuild failed for sourceService={}", sourceService, e);
            currentStatus.set(RebuildStatusDTO.failed("rebuild " + sourceService + " failed: " + e.getMessage()));
            rollbackShadow();
        }
    }

    /**
     * 获取当前重建状态。
     */
    public RebuildStatusDTO getStatus() {
        return currentStatus.get();
    }

    // --- 内部方法 ---

    private long recordSnapshotToken(String sourceService) {
        Long maxSeq = changeLogRepository.findMaxSequenceBySourceService(sourceService);
        return maxSeq != null ? maxSeq : 0L;
    }

    private void dropShadowTable() {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + SHADOW_TABLE);
        } catch (Exception e) {
            log.warn("Failed to drop shadow table: {}", e.getMessage());
        }
    }

    private void createShadowTable() {
        jdbcTemplate.execute("CREATE TABLE " + SHADOW_TABLE + " LIKE " + MAIN_TABLE);
    }

    private long writeMerchantToShadow(List<JsonNode> projections) {
        long count = 0;
        for (JsonNode node : projections) {
            StoreOfferProjection p = buildMerchantProjection(node);
            if (p != null) {
                insertIntoShadow(p);
                count++;
            }
        }
        return count;
    }

    private long writeOfferToShadow(List<JsonNode> projections) {
        long count = 0;
        for (JsonNode node : projections) {
            StoreOfferProjection p = buildOfferProjection(node);
            if (p != null) {
                insertIntoShadow(p);
                count++;
            }
        }
        return count;
    }

    private StoreOfferProjection buildMerchantProjection(JsonNode node) {
        StoreOfferProjection p = new StoreOfferProjection();
        p.setStoreId(node.has("storeId") ? node.get("storeId").asLong() : null);
        p.setOfferId(node.has("offerId") ? node.get("offerId").asLong() : 0L);
        p.setStoreName(node.has("storeName") && !node.get("storeName").isNull() ? node.get("storeName").asText() : null);
        p.setStoreAddress(node.has("storeAddress") && !node.get("storeAddress").isNull() ? node.get("storeAddress").asText() : null);
        p.setStoreLat(node.has("storeLat") && !node.get("storeLat").isNull() ? node.get("storeLat").asDouble() : null);
        p.setStoreLng(node.has("storeLng") && !node.get("storeLng").isNull() ? node.get("storeLng").asDouble() : null);
        p.setStorePhone(node.has("storePhone") && !node.get("storePhone").isNull() ? node.get("storePhone").asText() : null);
        p.setStoreStatus(node.has("storeStatus") && !node.get("storeStatus").isNull() ? node.get("storeStatus").asText() : "ACTIVE");
        p.setMerchantId(node.has("merchantId") && !node.get("merchantId").isNull() ? node.get("merchantId").asLong() : null);
        p.setMerchantName(node.has("merchantName") && !node.get("merchantName").isNull() ? node.get("merchantName").asText() : null);
        p.setAggregateVersion(1);
        p.setLastEventType("com.campusfruit.rebuild.MerchantRebuild");
        p.setLastEventAt(Instant.now());
        return p;
    }

    private StoreOfferProjection buildOfferProjection(JsonNode node) {
        StoreOfferProjection p = new StoreOfferProjection();
        p.setStoreId(node.has("storeId") ? node.get("storeId").asLong() : null);
        p.setOfferId(node.has("offerId") ? node.get("offerId").asLong() : null);
        p.setStoreName(node.has("storeName") && !node.get("storeName").isNull() ? node.get("storeName").asText() : null);
        p.setStoreAddress(node.has("storeAddress") && !node.get("storeAddress").isNull() ? node.get("storeAddress").asText() : null);
        p.setStoreLat(node.has("storeLat") && !node.get("storeLat").isNull() ? node.get("storeLat").asDouble() : null);
        p.setStoreLng(node.has("storeLng") && !node.get("storeLng").isNull() ? node.get("storeLng").asDouble() : null);
        p.setStorePhone(node.has("storePhone") && !node.get("storePhone").isNull() ? node.get("storePhone").asText() : null);
        p.setStoreStatus(node.has("storeStatus") && !node.get("storeStatus").isNull() ? node.get("storeStatus").asText() : null);
        p.setMerchantId(node.has("merchantId") && !node.get("merchantId").isNull() ? node.get("merchantId").asLong() : null);
        p.setMerchantName(node.has("merchantName") && !node.get("merchantName").isNull() ? node.get("merchantName").asText() : null);
        p.setCanonicalFruitId(node.has("canonicalFruitId") && !node.get("canonicalFruitId").isNull() ? node.get("canonicalFruitId").asLong() : null);
        p.setFruitCategory(node.has("fruitCategory") && !node.get("fruitCategory").isNull() ? node.get("fruitCategory").asText() : null);
        p.setFruitVariety(node.has("fruitVariety") && !node.get("fruitVariety").isNull() ? node.get("fruitVariety").asText() : null);
        p.setFruitGrade(node.has("fruitGrade") && !node.get("fruitGrade").isNull() ? node.get("fruitGrade").asText() : null);
        p.setFruitOrigin(node.has("fruitOrigin") && !node.get("fruitOrigin").isNull() ? node.get("fruitOrigin").asText() : null);
        p.setSalesUnit(node.has("salesUnit") && !node.get("salesUnit").isNull() ? node.get("salesUnit").asText() : null);
        p.setNetWeightGrams(node.has("netWeightGrams") && !node.get("netWeightGrams").isNull() ? node.get("netWeightGrams").asInt() : null);
        p.setUnitPrice(node.has("unitPrice") && !node.get("unitPrice").isNull() ? node.get("unitPrice").asLong() : null);
        if (node.has("standardPricePer500g") && !node.get("standardPricePer500g").isNull()) {
            p.setStandardPricePer500g(new java.math.BigDecimal(node.get("standardPricePer500g").asText()));
        }
        p.setIsComparable(node.has("isComparable") && !node.get("isComparable").isNull() ? node.get("isComparable").asBoolean() : true);
        p.setAvailableQuantity(node.has("availableQuantity") && !node.get("availableQuantity").isNull() ? node.get("availableQuantity").asInt() : 0);
        p.setOfferStatus(node.has("offerStatus") && !node.get("offerStatus").isNull() ? node.get("offerStatus").asText() : "ACTIVE");
        p.setPriceStale(node.has("priceStale") && !node.get("priceStale").isNull() ? node.get("priceStale").asBoolean() : false);
        if (node.has("avgRating") && !node.get("avgRating").isNull()) {
            p.setAvgRating(new java.math.BigDecimal(node.get("avgRating").asText()));
        }
        p.setReviewCount(node.has("reviewCount") && !node.get("reviewCount").isNull() ? node.get("reviewCount").asInt() : 0);
        p.setAggregateVersion(1);
        p.setLastEventType("com.campusfruit.rebuild.OfferRebuild");
        p.setLastEventAt(Instant.now());
        return p;
    }

    private void insertIntoShadow(StoreOfferProjection projection) {
        jdbcTemplate.update(
                "INSERT INTO " + SHADOW_TABLE + " (store_id, offer_id, store_name, store_address, store_lat, store_lng, " +
                "store_phone, store_status, merchant_id, merchant_name, canonical_fruit_id, fruit_category, " +
                "fruit_variety, fruit_grade, fruit_origin, sales_unit, net_weight_grams, unit_price, " +
                "standard_price_per500g, is_comparable, available_quantity, offer_status, price_stale, " +
                "avg_rating, review_count, aggregate_version, last_event_type, last_event_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                projection.getStoreId(), projection.getOfferId(), projection.getStoreName(),
                projection.getStoreAddress(), projection.getStoreLat(), projection.getStoreLng(),
                projection.getStorePhone(), projection.getStoreStatus(), projection.getMerchantId(),
                projection.getMerchantName(), projection.getCanonicalFruitId(), projection.getFruitCategory(),
                projection.getFruitVariety(), projection.getFruitGrade(), projection.getFruitOrigin(),
                projection.getSalesUnit(), projection.getNetWeightGrams(), projection.getUnitPrice(),
                projection.getStandardPricePer500g(), projection.getIsComparable(), projection.getAvailableQuantity(),
                projection.getOfferStatus(), projection.getPriceStale(), projection.getAvgRating(),
                projection.getReviewCount(), projection.getAggregateVersion(),
                projection.getLastEventType(), projection.getLastEventAt()
        );
    }

    private long pullDelta(String sourceService, long fromSequence) {
        List<ProjectionChangeLog> deltas = changeLogRepository
                .findBySourceServiceAndSequenceGreaterThanOrderBySequenceAsc(sourceService, fromSequence);

        long applied = 0;
        for (ProjectionChangeLog delta : deltas) {
            applyDelta(delta);
            applied++;
        }

        long highWatermark = fromSequence;
        if (!deltas.isEmpty()) {
            highWatermark = deltas.get(deltas.size() - 1).getSequence();
        }

        return highWatermark;
    }

    private void applyDelta(ProjectionChangeLog delta) {
        try {
            if ("INSERT".equals(delta.getOperation()) || "UPDATE".equals(delta.getOperation())) {
                if (delta.getAfterSnapshot() != null) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    JsonNode node = mapper.readTree(delta.getAfterSnapshot());
                    StoreOfferProjection p;
                    if ("merchant-service".equals(delta.getSourceService())) {
                        p = buildMerchantProjection(node);
                    } else {
                        p = buildOfferProjection(node);
                    }
                    if (p != null) {
                        // 检查影子表中是否已有该 offer，有则更新，无则插入
                        Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM " + SHADOW_TABLE + " WHERE offer_id = ?",
                                Integer.class, p.getOfferId());
                        if (count != null && count > 0) {
                            updateShadow(p);
                        } else {
                            insertIntoShadow(p);
                        }
                    }
                }
            } else if ("DELETE".equals(delta.getOperation())) {
                jdbcTemplate.update("DELETE FROM " + SHADOW_TABLE + " WHERE offer_id = ?",
                        Long.parseLong(delta.getAggregateId()));
            }
        } catch (Exception e) {
            log.warn("Failed to apply delta: sequence={}", delta.getSequence(), e);
        }
    }

    private void updateShadow(StoreOfferProjection projection) {
        jdbcTemplate.update(
                "UPDATE " + SHADOW_TABLE + " SET store_name=?, store_address=?, store_lat=?, store_lng=?, " +
                "store_phone=?, store_status=?, merchant_id=?, merchant_name=?, canonical_fruit_id=?, " +
                "fruit_category=?, fruit_variety=?, fruit_grade=?, fruit_origin=?, sales_unit=?, " +
                "net_weight_grams=?, unit_price=?, standard_price_per500g=?, is_comparable=?, " +
                "available_quantity=?, offer_status=?, price_stale=?, avg_rating=?, review_count=?, " +
                "aggregate_version=?, last_event_type=?, last_event_at=? WHERE offer_id=?",
                projection.getStoreName(), projection.getStoreAddress(), projection.getStoreLat(),
                projection.getStoreLng(), projection.getStorePhone(), projection.getStoreStatus(),
                projection.getMerchantId(), projection.getMerchantName(), projection.getCanonicalFruitId(),
                projection.getFruitCategory(), projection.getFruitVariety(), projection.getFruitGrade(),
                projection.getFruitOrigin(), projection.getSalesUnit(), projection.getNetWeightGrams(),
                projection.getUnitPrice(), projection.getStandardPricePer500g(), projection.getIsComparable(),
                projection.getAvailableQuantity(), projection.getOfferStatus(), projection.getPriceStale(),
                projection.getAvgRating(), projection.getReviewCount(), projection.getAggregateVersion(),
                projection.getLastEventType(), projection.getLastEventAt(), projection.getOfferId()
        );
    }

    private boolean checkSequenceGaps(String sourceService) {
        List<ProjectionChangeLog> all = changeLogRepository.findBySourceServiceOrderBySequenceAsc(sourceService);
        if (all.isEmpty()) {
            return false;
        }

        long expected = all.get(0).getSequence();
        for (ProjectionChangeLog changeLog : all) {
            if (changeLog.getSequence() != expected) {
                log.warn("Sequence gap detected: expected={}, actual={}", expected, changeLog.getSequence());
                return true;
            }
            expected++;
        }
        return false;
    }

    private void atomicSwap() {
        log.info("Atomic swap: {} -> {}, {} -> {} (old)", MAIN_TABLE, OLD_TABLE, SHADOW_TABLE, MAIN_TABLE);
        jdbcTemplate.execute("RENAME TABLE " + MAIN_TABLE + " TO " + OLD_TABLE + ", " +
                SHADOW_TABLE + " TO " + MAIN_TABLE);
        dropOldTable();
    }

    private void dropOldTable() {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + OLD_TABLE);
        } catch (Exception e) {
            log.warn("Failed to drop old table: {}", e.getMessage());
        }
    }

    private void rollbackShadow() {
        try {
            dropShadowTable();
        } catch (Exception e) {
            log.warn("Failed to rollback shadow table: {}", e.getMessage());
        }
    }
}
