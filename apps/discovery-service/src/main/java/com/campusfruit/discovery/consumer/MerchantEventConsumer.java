package com.campusfruit.discovery.consumer;

import com.campusfruit.discovery.client.MerchantServiceClient;
import com.campusfruit.discovery.entity.ProjectionCheckpoint;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.ProjectionCheckpointRepository;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import com.campusfruit.events.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class MerchantEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MerchantEventConsumer.class);
    private static final String SOURCE = "merchant-service";

    private final StoreOfferProjectionRepository projectionRepository;
    private final ProjectionCheckpointRepository checkpointRepository;
    private final MerchantServiceClient merchantServiceClient;
    private final ObjectMapper objectMapper;

    public MerchantEventConsumer(StoreOfferProjectionRepository projectionRepository,
                                  ProjectionCheckpointRepository checkpointRepository,
                                  MerchantServiceClient merchantServiceClient,
                                  ObjectMapper objectMapper) {
        this.projectionRepository = projectionRepository;
        this.checkpointRepository = checkpointRepository;
        this.merchantServiceClient = merchantServiceClient;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "discovery.merchant")
    @Transactional
    public void onMerchantEvent(EventEnvelope envelope) {
        if (envelope == null) {
            log.warn("Received null envelope");
            return;
        }

        String eventType = envelope.getEventType();
        log.debug("Received merchant event: type={}, aggregateId={}, version={}",
                eventType, envelope.getAggregateId(), envelope.getAggregateVersion());

        // 幂等检查：按 aggregateVersion 去重
        ProjectionCheckpoint checkpoint = getOrCreateCheckpoint();
        if (envelope.getAggregateVersion() < checkpoint.getLastSourceSequence()) {
            log.debug("Skipping stale event: version={} < checkpoint={}",
                    envelope.getAggregateVersion(), checkpoint.getLastSourceSequence());
            return;
        }

        try {
            // tombstone 处理：删除对应投影记录
            if (envelope.isTombstone()) {
                handleTombstone(envelope);
                updateCheckpoint(checkpoint, envelope);
                return;
            }

            switch (eventType) {
                case "com.campusfruit.merchant.MerchantApproved":
                    handleMerchantApproved(envelope);
                    break;
                case "com.campusfruit.merchant.StoreActivated":
                    handleStoreActivated(envelope);
                    break;
                case "com.campusfruit.merchant.StoreSuspended":
                    handleStoreSuspended(envelope);
                    break;
                case "com.campusfruit.merchant.StoreLocationChanged":
                    handleStoreLocationChanged(envelope);
                    break;
                default:
                    log.debug("Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process merchant event: type={}, aggregateId={}",
                    eventType, envelope.getAggregateId(), e);
            throw new RuntimeException("Event processing failed", e);
        }

        // 更新 checkpoint
        updateCheckpoint(checkpoint, envelope);
    }

    private void handleMerchantApproved(EventEnvelope envelope) throws Exception {
        // payload 是 Merchant 实体 JSON，包含 id, name 等字段
        JsonNode payload = objectMapper.readTree(envelope.getPayload());
        Long merchantId = payload.get("id").asLong();
        String merchantName = payload.has("name") && !payload.get("name").isNull()
                ? payload.get("name").asText() : "";

        // 更新该 merchant 下所有门店投影的 merchant_name 和 merchant_status
        List<StoreOfferProjection> projections = projectionRepository.findAll();
        for (StoreOfferProjection p : projections) {
            if (merchantId.equals(p.getMerchantId())) {
                p.setMerchantName(merchantName);
                p.setMerchantStatus("APPROVED");
                p.setLastEventType(envelope.getEventType());
                p.setLastEventAt(Instant.now());
                projectionRepository.save(p);
            }
        }

        log.info("MerchantApproved: updated merchant_name and status for merchantId={}", merchantId);
    }

    private void handleStoreActivated(EventEnvelope envelope) throws Exception {
        JsonNode payload = objectMapper.readTree(envelope.getPayload());
        Long storeId = payload.get("id").asLong();

        // 提取 Store 字段
        String storeName = payload.has("name") && !payload.get("name").isNull()
                ? payload.get("name").asText() : "";
        String address = payload.has("address") && !payload.get("address").isNull()
                ? payload.get("address").asText() : null;
        Double lat = payload.has("latitude") && !payload.get("latitude").isNull()
                ? payload.get("latitude").asDouble() : null;
        Double lng = payload.has("longitude") && !payload.get("longitude").isNull()
                ? payload.get("longitude").asDouble() : null;
        String phone = payload.has("phone") && !payload.get("phone").isNull()
                ? payload.get("phone").asText() : null;
        String status = payload.has("status") && !payload.get("status").isNull()
                ? payload.get("status").asText() : "ACTIVE";

        // 提取 merchant 信息（Store 中 merchant 是嵌套对象）
        Long merchantId = null;
        String merchantName = null;
        if (payload.has("merchant") && !payload.get("merchant").isNull()) {
            JsonNode merchantNode = payload.get("merchant");
            merchantId = merchantNode.has("id") ? merchantNode.get("id").asLong() : null;
            merchantName = merchantNode.has("name") && !merchantNode.get("name").isNull()
                    ? merchantNode.get("name").asText() : null;
        }

        // 更新或创建该门店的所有投影记录
        List<StoreOfferProjection> projections = projectionRepository.findByStoreId(storeId);
        if (projections.isEmpty()) {
            // 尝试从 Merchant projection export 获取门店信息填充
            JsonNode merchantProjection = null;
            try {
                merchantProjection = merchantServiceClient.fetchStoreProjection(storeId);
            } catch (Exception e) {
                log.warn("Failed to fetch store projection for storeId={}, falling back to event payload", storeId);
            }

            // 如果 API 调用成功，优先使用 projection export 的数据
            String effectiveStoreName = storeName;
            String effectiveAddress = address;
            Double effectiveLat = lat;
            Double effectiveLng = lng;
            String effectivePhone = phone;
            String effectiveStatus = status;
            Long effMerchantId = merchantId;
            String effMerchantName = merchantName;

            if (merchantProjection != null) {
                if (merchantProjection.has("storeName") && !merchantProjection.get("storeName").isNull()) {
                    effectiveStoreName = merchantProjection.get("storeName").asText();
                }
                if (merchantProjection.has("storeAddress") && !merchantProjection.get("storeAddress").isNull()) {
                    effectiveAddress = merchantProjection.get("storeAddress").asText();
                }
                if (merchantProjection.has("storeLat") && !merchantProjection.get("storeLat").isNull()) {
                    effectiveLat = merchantProjection.get("storeLat").asDouble();
                }
                if (merchantProjection.has("storeLng") && !merchantProjection.get("storeLng").isNull()) {
                    effectiveLng = merchantProjection.get("storeLng").asDouble();
                }
                if (merchantProjection.has("storePhone") && !merchantProjection.get("storePhone").isNull()) {
                    effectivePhone = merchantProjection.get("storePhone").asText();
                }
                if (merchantProjection.has("storeStatus") && !merchantProjection.get("storeStatus").isNull()) {
                    effectiveStatus = merchantProjection.get("storeStatus").asText();
                }
                if (merchantProjection.has("merchantId") && !merchantProjection.get("merchantId").isNull()) {
                    effMerchantId = merchantProjection.get("merchantId").asLong();
                }
                if (merchantProjection.has("merchantName") && !merchantProjection.get("merchantName").isNull()) {
                    effMerchantName = merchantProjection.get("merchantName").asText();
                }
            }

            // 创建一条仅包含门店信息的投影（等 Offer 事件来补全水果信息）
            StoreOfferProjection p = new StoreOfferProjection();
            p.setStoreId(storeId);
            p.setOfferId(0L);
            p.setStoreName(effectiveStoreName);
            p.setStoreAddress(effectiveAddress);
            p.setStoreLat(effectiveLat);
            p.setStoreLng(effectiveLng);
            p.setStorePhone(effectivePhone);
            p.setStoreStatus(effectiveStatus);
            p.setMerchantId(effMerchantId);
            p.setMerchantName(effMerchantName);
            p.setAggregateVersion((int) envelope.getAggregateVersion());
            p.setLastEventType(envelope.getEventType());
            p.setLastEventAt(Instant.now());
            projectionRepository.save(p);
            log.info("StoreActivated: created store projection for storeId={} (source: {})",
                    storeId, merchantProjection != null ? "projection-export" : "event-payload");
        } else {
            // 更新已有投影中的门店信息
            for (StoreOfferProjection p : projections) {
                if (envelope.getAggregateVersion() < p.getAggregateVersion()) {
                    continue;
                }
                p.setStoreName(storeName);
                p.setStoreAddress(address);
                p.setStoreLat(lat);
                p.setStoreLng(lng);
                p.setStorePhone(phone);
                p.setStoreStatus(status);
                p.setMerchantId(merchantId);
                p.setMerchantName(merchantName);
                p.setAggregateVersion((int) envelope.getAggregateVersion());
                p.setLastEventType(envelope.getEventType());
                p.setLastEventAt(Instant.now());
                projectionRepository.save(p);
            }
            log.info("StoreActivated: updated {} projections for storeId={}", projections.size(), storeId);
        }
    }

    private void handleStoreSuspended(EventEnvelope envelope) throws Exception {
        JsonNode payload = objectMapper.readTree(envelope.getPayload());
        Long storeId = payload.get("id").asLong();

        List<StoreOfferProjection> projections = projectionRepository.findByStoreId(storeId);
        for (StoreOfferProjection p : projections) {
            if (envelope.getAggregateVersion() < p.getAggregateVersion()) {
                continue;
            }
            p.setStoreStatus("SUSPENDED");
            p.setAggregateVersion((int) envelope.getAggregateVersion());
            p.setLastEventType(envelope.getEventType());
            p.setLastEventAt(Instant.now());
            projectionRepository.save(p);
        }

        log.info("StoreSuspended: suspended {} projections for storeId={}", projections.size(), storeId);
    }

    private void handleStoreLocationChanged(EventEnvelope envelope) throws Exception {
        JsonNode payload = objectMapper.readTree(envelope.getPayload());
        Long storeId = payload.get("id").asLong();
        Double lat = payload.has("latitude") && !payload.get("latitude").isNull()
                ? payload.get("latitude").asDouble() : null;
        Double lng = payload.has("longitude") && !payload.get("longitude").isNull()
                ? payload.get("longitude").asDouble() : null;

        List<StoreOfferProjection> projections = projectionRepository.findByStoreId(storeId);
        for (StoreOfferProjection p : projections) {
            if (envelope.getAggregateVersion() < p.getAggregateVersion()) {
                continue;
            }
            p.setStoreLat(lat);
            p.setStoreLng(lng);
            p.setAggregateVersion((int) envelope.getAggregateVersion());
            p.setLastEventType(envelope.getEventType());
            p.setLastEventAt(Instant.now());
            projectionRepository.save(p);
        }

        log.info("StoreLocationChanged: updated {} projections for storeId={}", projections.size(), storeId);
    }

    private void handleTombstone(EventEnvelope envelope) {
        try {
            long aggregateId = Long.parseLong(envelope.getAggregateId());
            List<StoreOfferProjection> projections = projectionRepository.findByStoreId(aggregateId);
            for (StoreOfferProjection p : projections) {
                projectionRepository.delete(p);
                log.info("Tombstone: deleted projection for storeId={}, offerId={}", aggregateId, p.getOfferId());
            }
        } catch (NumberFormatException e) {
            log.warn("Tombstone: invalid aggregateId format: {}", envelope.getAggregateId());
        }
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
