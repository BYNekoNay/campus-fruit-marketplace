package com.campusfruit.offer.service;

import com.campusfruit.events.EventEnvelope;
import com.campusfruit.offer.entity.Offer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OfferEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OfferEventPublisher.class);
    private static final String PRODUCER = "offer-service";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.offer.events.exchange:campus-fruit.events}")
    private String exchange;

    public OfferEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 报价创建事件。
     */
    public void publishOfferCreated(Offer offer) {
        EventEnvelope envelope = buildEnvelope("com.campusfruit.offer.OfferCreated",
                "Offer", String.valueOf(offer.getId()));
        try {
            envelope.setPayload(objectMapper.writeValueAsString(offer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize offer payload", e);
            return;
        }
        publish(envelope, "offer.created");
    }

    /**
     * 报价价格变更事件。
     */
    public void publishOfferPriceChanged(Offer offer, Long oldPrice) {
        EventEnvelope envelope = buildEnvelope("com.campusfruit.offer.OfferPriceChanged",
                "Offer", String.valueOf(offer.getId()));
        try {
            Payload payload = new Payload();
            payload.setOfferId(offer.getId());
            payload.setNewPrice(offer.getUnitPrice());
            payload.setOldPrice(oldPrice);
            payload.setNetWeightGrams(offer.getNetWeightGrams());
            payload.setSalesUnit(offer.getSalesUnit());
            payload.setChangedAt(offer.getUpdatedAt() != null ? offer.getUpdatedAt().toString() : null);
            envelope.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize price change payload", e);
            return;
        }
        publish(envelope, "offer.price.changed");
    }

    /**
     * 报价库存变更事件。
     */
    public void publishOfferStockChanged(Offer offer) {
        EventEnvelope envelope = buildEnvelope("com.campusfruit.offer.OfferStockChanged",
                "Offer", String.valueOf(offer.getId()));
        try {
            Payload payload = new Payload();
            payload.setOfferId(offer.getId());
            payload.setAvailableQuantity(offer.getAvailableQuantity());
            payload.setReservedQuantity(offer.getReservedQuantity());
            payload.setTotalQuantity(offer.getStockQuantity());
            envelope.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize stock change payload", e);
            return;
        }
        publish(envelope, "offer.stock.changed");
    }

    /**
     * 报价状态变更事件。
     */
    public void publishOfferStatusChanged(Offer offer) {
        EventEnvelope envelope = buildEnvelope("com.campusfruit.offer.OfferStatusChanged",
                "Offer", String.valueOf(offer.getId()));
        try {
            Payload payload = new Payload();
            payload.setOfferId(offer.getId());
            payload.setNewStatus(offer.getStatus().name());
            envelope.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize status change payload", e);
            return;
        }
        publish(envelope, "offer.status.changed");
    }

    private EventEnvelope buildEnvelope(String eventType, String aggregateType, String aggregateId) {
        EventEnvelope envelope = new EventEnvelope();
        envelope.setEventType(eventType);
        envelope.setProducer(PRODUCER);
        envelope.setAggregateType(aggregateType);
        envelope.setAggregateId(aggregateId);
        return envelope;
    }

    private void publish(EventEnvelope envelope, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, envelope);
            log.debug("Published event: type={}, routingKey={}", envelope.getEventType(), routingKey);
        } catch (Exception e) {
            log.error("Failed to publish event: type={}, routingKey={}", envelope.getEventType(), routingKey, e);
        }
    }

    /**
     * 内部简单的 payload 类。
     */
    private static class Payload {
        private Long offerId;
        private Long newPrice;
        private Long oldPrice;
        private Integer netWeightGrams;
        private String salesUnit;
        private String changedAt;
        private Integer availableQuantity;
        private Integer reservedQuantity;
        private Integer totalQuantity;
        private String newStatus;

        public Long getOfferId() { return offerId; }
        public void setOfferId(Long offerId) { this.offerId = offerId; }

        public Long getNewPrice() { return newPrice; }
        public void setNewPrice(Long newPrice) { this.newPrice = newPrice; }

        public Long getOldPrice() { return oldPrice; }
        public void setOldPrice(Long oldPrice) { this.oldPrice = oldPrice; }

        public Integer getNetWeightGrams() { return netWeightGrams; }
        public void setNetWeightGrams(Integer netWeightGrams) { this.netWeightGrams = netWeightGrams; }

        public String getSalesUnit() { return salesUnit; }
        public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }

        public String getChangedAt() { return changedAt; }
        public void setChangedAt(String changedAt) { this.changedAt = changedAt; }

        public Integer getAvailableQuantity() { return availableQuantity; }
        public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

        public Integer getReservedQuantity() { return reservedQuantity; }
        public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

        public Integer getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

        public String getNewStatus() { return newStatus; }
        public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    }
}
