package com.campusfruit.review.consumer;

import com.campusfruit.review.entity.ReviewEligibility;
import com.campusfruit.review.repository.ReviewEligibilityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * 订单事件消费者。监听订单完成/取消事件，管理评价资格。
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ReviewEligibilityRepository eligibilityRepository;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(ReviewEligibilityRepository eligibilityRepository,
                               ObjectMapper objectMapper) {
        this.eligibilityRepository = eligibilityRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "review.order")
    @Transactional
    public void handleOrderEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);

            // 从消息中提取字段
            String eventType = event.has("eventType") ? event.get("eventType").asText()
                    : event.has("toStatus") ? event.get("toStatus").asText() : null;

            Long orderId = null;
            Long userId = null;
            Long storeId = null;

            if (event.has("orderId")) {
                orderId = event.get("orderId").asLong();
            } else if (event.has("payload")) {
                JsonNode payload = objectMapper.readTree(event.get("payload").asText());
                orderId = payload.has("orderId") ? payload.get("orderId").asLong() : null;
                userId = payload.has("userId") ? payload.get("userId").asLong() : null;
                storeId = payload.has("storeId") ? payload.get("storeId").asLong() : null;
            }

            if (orderId == null) {
                log.warn("无法从事件中提取 orderId: {}", message);
                return;
            }

            if ("COMPLETED".equals(eventType) || "com.campusfruit.order.OrderCompleted".equals(eventType)) {
                handleOrderCompleted(orderId, userId, storeId);
            } else if ("CANCELLED".equals(eventType) || "com.campusfruit.order.OrderCancelled".equals(eventType)) {
                handleOrderCancelled(orderId);
            }

        } catch (Exception e) {
            log.error("处理订单事件失败: {}", e.getMessage(), e);
        }
    }

    private void handleOrderCompleted(Long orderId, Long userId, Long storeId) {
        // 幂等检查：此订单是否已有资格记录
        Optional<ReviewEligibility> existing = eligibilityRepository.findByOrderId(orderId);
        if (existing.isEmpty()) {
            ReviewEligibility eligibility = new ReviewEligibility();
            eligibility.setOrderId(orderId);
            eligibility.setUserId(userId != null ? userId : 0L);
            eligibility.setStoreId(storeId != null ? storeId : 0L);
            eligibility.setUsed(false);
            eligibility.setTombstone(false);
            eligibility.setOrderCompletedAt(Instant.now());
            eligibilityRepository.save(eligibility);
            log.info("创建评价资格: orderId={}, userId={}, storeId={}", orderId, userId, storeId);
        } else {
            log.debug("评价资格已存在，跳过: orderId={}", orderId);
        }
    }

    private void handleOrderCancelled(Long orderId) {
        // 标记相应资格为墓碑
        Optional<ReviewEligibility> existing = eligibilityRepository.findByOrderId(orderId);
        existing.ifPresent(eligibility -> {
            if (!eligibility.getTombstone()) {
                eligibility.setTombstone(true);
                eligibilityRepository.save(eligibility);
                log.info("标记评价资格为墓碑: orderId={}", orderId);
            }
        });
    }
}
