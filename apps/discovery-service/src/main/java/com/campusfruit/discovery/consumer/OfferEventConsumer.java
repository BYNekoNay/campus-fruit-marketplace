package com.campusfruit.discovery.consumer;

import com.campusfruit.discovery.entity.ProjectionCheckpoint;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.ProjectionCheckpointRepository;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import com.campusfruit.discovery.service.PriceStatsUpdateService;
import com.campusfruit.events.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Component
public class OfferEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OfferEventConsumer.class);
    private static final String SOURCE = "offer-service";
    private static final BigDecimal FIVE_HUNDRED = BigDecimal.valueOf(500);

    private final StoreOfferProjectionRepository projectionRepository;
    private final ProjectionCheckpointRepository checkpointRepository;
    private final PriceStatsUpdateService priceStatsUpdateService;
    private final ObjectMapper objectMapper;

    public OfferEventConsumer(StoreOfferProjectionRepository projectionRepository,
                               ProjectionCheckpointRepository checkpointRepository,
                               PriceStatsUpdateService priceStatsUpdateService,
                               ObjectMapper objectMapper) {
        this.projectionRepository = projectionRepository;
        this.checkpointRepository = checkpointRepository;
        this.priceStatsUpdateService = priceStatsUpdateService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "discovery.offer")
    @Transactional
    public void onOfferEvent(EventEnvelope envelope) {
        if (envelope == null) {
            log.warn("Received null envelope");
            return;
        }

        String eventType = envelope.getEventType();
        log.debug("Received offer event: type={}, aggregateId={}, version={}",
                eventType, envelope.getAggregateId(), envelope.getAggregateVersion());

        // 幂等检查
        ProjectionCheckpoint checkpoint = getOrCreateCheckpoint();
        if (envelope.getAggregateVersion() < checkpoint.getLastSourceSequence()) {
            log.debug("Skipping stale event: version={} < checkpoint={}",
                    envelope.getAggregateVersion(), checkpoint.getLastSourceSequence());
            return;
        }

        try {
            // tombstone 处理：删除对应报价投影记录
            if (envelope.isTombstone()) {
                handleTombstone(envelope);
                updateCheckpoint(checkpoint, envelope);
                return;
            }

            switch (eventType) {
                case "com.campusfruit.offer.OfferCreated":
                    handleOfferCreated(envelope);
                    break;
                case "com.campusfruit.offer.OfferPriceChanged":
                    handleOfferPriceChanged(envelope);
                    break;
                case "com.campusfruit.offer.OfferStockChanged":
                    handleOfferStockChanged(envelope);
                    break;
                case "com.campusfruit.offer.OfferStatusChanged":
                    handleOfferStatusChanged(envelope);
                    break;
                default:
                    log.debug("Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process offer event: type={}, aggregateId={}",
                    eventType, envelope.getAggregateId(), e);
            throw new RuntimeException("Event processing failed", e);
        }

        updateCheckpoint(checkpoint, envelope);
    }

    private void handleOfferCreated(EventEnvelope envelope) throws Exception {
        // payload 是 Offer 实体 JSON，包含 storeId, canonicalFruit, salesUnit, netWeightGrams, unitPrice 等
        JsonNode payload = objectMapper.readTree(envelope.getPayload());
        Long offerId = payload.get("id").asLong();
        Long storeId = payload.get("storeId").asLong();

        // 检查是否已存在（幂等）
        if (projectionRepository.findByOfferId(offerId).isPresent()) {
            log.debug("Offer projection already exists for offerId={}, updating", offerId);
        }

        StoreOfferProjection projection = projectionRepository.findByOfferId(offerId)
                .orElse(new StoreOfferProjection());

        // 报价信息
        projection.setOfferId(offerId);
        projection.setStoreId(storeId);

        String salesUnit = payload.has("salesUnit") && !payload.get("salesUnit").isNull()
                ? payload.get("salesUnit").asText() : null;
        projection.setSalesUnit(salesUnit);

        if (payload.has("netWeightGrams") && !payload.get("netWeightGrams").isNull()) {
            projection.setNetWeightGrams(payload.get("netWeightGrams").asInt());
        }

        if (payload.has("unitPrice") && !payload.get("unitPrice").isNull()) {
            projection.setUnitPrice(payload.get("unitPrice").asLong());
        }

        // 计算标准价格
        computeStandardPrice(projection);

        if (payload.has("availableQuantity") && !payload.get("availableQuantity").isNull()) {
            projection.setAvailableQuantity(payload.get("availableQuantity").asInt());
        }

        String offerStatus = payload.has("status") && !payload.get("status").isNull()
                ? payload.get("status").asText() : "ACTIVE";
        projection.setOfferStatus(offerStatus);

        if (payload.has("priceStale") && !payload.get("priceStale").isNull()) {
            projection.setPriceStale(payload.get("priceStale").asBoolean());
        }

        // 水果信息（从嵌套 canonicalFruit 提取）
        if (payload.has("canonicalFruit") && !payload.get("canonicalFruit").isNull()) {
            JsonNode fruit = payload.get("canonicalFruit");
            projection.setCanonicalFruitId(fruit.has("id") ? fruit.get("id").asLong() : null);
            projection.setFruitCategory(fruit.has("category") && !fruit.get("category").isNull()
                    ? fruit.get("category").asText() : null);
            projection.setFruitVariety(fruit.has("variety") && !fruit.get("variety").isNull()
                    ? fruit.get("variety").asText() : null);
            projection.setFruitGrade(fruit.has("grade") && !fruit.get("grade").isNull()
                    ? fruit.get("grade").asText() : null);
            projection.setFruitOrigin(fruit.has("origin") && !fruit.get("origin").isNull()
                    ? fruit.get("origin").asText() : null);
        }

        // 从已有门店投影复制门店信息（如果存在）
        List<StoreOfferProjection> storeProjections = projectionRepository.findByStoreId(storeId);
        if (!storeProjections.isEmpty()) {
            StoreOfferProjection storeRef = storeProjections.get(0);
            projection.setStoreName(storeRef.getStoreName());
            projection.setStoreAddress(storeRef.getStoreAddress());
            projection.setStoreLat(storeRef.getStoreLat());
            projection.setStoreLng(storeRef.getStoreLng());
            projection.setStorePhone(storeRef.getStorePhone());
            projection.setStoreStatus(storeRef.getStoreStatus());
            projection.setMerchantId(storeRef.getMerchantId());
            projection.setMerchantName(storeRef.getMerchantName());
        }

        projection.setAggregateVersion((int) envelope.getAggregateVersion());
        projection.setLastEventType(envelope.getEventType());
        projection.setLastEventAt(Instant.now());
        projectionRepository.save(projection);

        // 异步更新价格统计
        if (projection.getCanonicalFruitId() != null) {
            priceStatsUpdateService.updateStatsAsync(projection.getCanonicalFruitId());
        }

        log.info("OfferCreated: created/updated projection for offerId={}, storeId={}", offerId, storeId);
    }

    private void handleOfferPriceChanged(EventEnvelope envelope) throws Exception {
        JsonNode payload = objectMapper.readTree(envelope.getPayload());
        Long offerId = payload.get("offerId").asLong();

        projectionRepository.findByOfferId(offerId).ifPresent(projection -> {
            if (envelope.getAggregateVersion() < projection.getAggregateVersion()) {
                return;
            }
            if (payload.has("newPrice") && !payload.get("newPrice").isNull()) {
                projection.setUnitPrice(payload.get("newPrice").asLong());
            }
            if (payload.has("netWeightGrams") && !payload.get("netWeightGrams").isNull()) {
                projection.setNetWeightGrams(payload.get("netWeightGrams").asInt());
            }
            if (payload.has("salesUnit") && !payload.get("salesUnit").isNull()) {
                projection.setSalesUnit(payload.get("salesUnit").asText());
            }

            computeStandardPrice(projection);
            projection.setAggregateVersion((int) envelope.getAggregateVersion());
            projection.setLastEventType(envelope.getEventType());
            projection.setLastEventAt(Instant.now());
            projectionRepository.save(projection);

            // 异步更新价格统计
            if (projection.getCanonicalFruitId() != null) {
                priceStatsUpdateService.updateStatsAsync(projection.getCanonicalFruitId());
            }

            log.info("OfferPriceChanged: updated price for offerId={}", offerId);
        });
    }

    private void handleOfferStockChanged(EventEnvelope envelope) throws Exception {
        JsonNode payload = objectMapper.readTree(envelope.getPayload());
        Long offerId = payload.get("offerId").asLong();

        projectionRepository.findByOfferId(offerId).ifPresent(projection -> {
            if (envelope.getAggregateVersion() < projection.getAggregateVersion()) {
                return;
            }
            if (payload.has("availableQuantity") && !payload.get("availableQuantity").isNull()) {
                projection.setAvailableQuantity(payload.get("availableQuantity").asInt());
            }
            projection.setAggregateVersion((int) envelope.getAggregateVersion());
            projection.setLastEventType(envelope.getEventType());
            projection.setLastEventAt(Instant.now());
            projectionRepository.save(projection);

            log.info("OfferStockChanged: updated stock for offerId={}", offerId);
        });
    }

    private void handleOfferStatusChanged(EventEnvelope envelope) throws Exception {
        JsonNode payload = objectMapper.readTree(envelope.getPayload());
        Long offerId = payload.get("offerId").asLong();
        String newStatus = payload.has("newStatus") && !payload.get("newStatus").isNull()
                ? payload.get("newStatus").asText() : null;

        projectionRepository.findByOfferId(offerId).ifPresent(projection -> {
            if (envelope.getAggregateVersion() < projection.getAggregateVersion()) {
                return;
            }
            projection.setOfferStatus(newStatus);
            projection.setAggregateVersion((int) envelope.getAggregateVersion());
            projection.setLastEventType(envelope.getEventType());
            projection.setLastEventAt(Instant.now());
            projectionRepository.save(projection);

            log.info("OfferStatusChanged: updated status to {} for offerId={}", newStatus, offerId);
        });
    }

    private void handleTombstone(EventEnvelope envelope) {
        try {
            long offerId = Long.parseLong(envelope.getAggregateId());
            projectionRepository.findByOfferId(offerId).ifPresent(projection -> {
                projectionRepository.delete(projection);
                log.info("Tombstone: deleted projection for offerId={}", offerId);
            });
        } catch (NumberFormatException e) {
            log.warn("Tombstone: invalid aggregateId format: {}", envelope.getAggregateId());
        }
    }

    private void computeStandardPrice(StoreOfferProjection projection) {
        if (projection.getNetWeightGrams() == null || projection.getNetWeightGrams() <= 0
                || projection.getUnitPrice() == null) {
            projection.setIsComparable(false);
            projection.setStandardPricePer500g(null);
            return;
        }

        BigDecimal priceInFen = BigDecimal.valueOf(projection.getUnitPrice());
        BigDecimal weight = BigDecimal.valueOf(projection.getNetWeightGrams());

        // standardPricePer500g(元) = unitPrice * 5 / netWeightGrams
        BigDecimal standardPrice = priceInFen.multiply(BigDecimal.valueOf(5))
                .divide(weight, 2, RoundingMode.HALF_UP);

        projection.setStandardPricePer500g(standardPrice);
        projection.setIsComparable(true);
    }

    private ProjectionCheckpoint getOrCreateCheckpoint() {
        return checkpointRepository.findBySourceService(SOURCE)
                .orElseGet(() -> {
                    ProjectionCheckpoint cp = new ProjectionCheckpoint(SOURCE);
                    cp.setLastSourceSequence(0L);
                    return checkpointRepository.save(cp);
                });
    }

    private void updateCheckpoint(ProjectionCheckpoint checkpoint, EventEnvelope envelope) {
        checkpoint.setLastEventId(envelope.getEventId() != null ? envelope.getEventId().toString() : null);
        checkpoint.setLastSourceSequence(envelope.getAggregateVersion());
        checkpoint.setUpdatedAt(Instant.now());
        checkpointRepository.save(checkpoint);
    }
}
