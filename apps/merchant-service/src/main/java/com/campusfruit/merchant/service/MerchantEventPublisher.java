package com.campusfruit.merchant.service;

import com.campusfruit.events.EventEnvelope;
import com.campusfruit.merchant.entity.Store;
import com.campusfruit.merchant.entity.Merchant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MerchantEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MerchantEventPublisher.class);
    private static final String PRODUCER = "merchant-service";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.merchant.events.exchange:campus-fruit.events}")
    private String exchange;

    public MerchantEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 商家审核通过事件。
     */
    public void publishMerchantApproved(Merchant merchant) {
        EventEnvelope envelope = buildEnvelope("com.campusfruit.merchant.MerchantApproved",
                "Merchant", String.valueOf(merchant.getId()));
        try {
            envelope.setPayload(objectMapper.writeValueAsString(merchant));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize merchant payload", e);
            return;
        }
        publish(envelope, "merchant.approved");
    }

    /**
     * 门店激活事件。
     */
    public void publishStoreActivated(Store store) {
        EventEnvelope envelope = buildEnvelope("com.campusfruit.merchant.StoreActivated",
                "Store", String.valueOf(store.getId()));
        try {
            envelope.setPayload(objectMapper.writeValueAsString(store));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize store payload", e);
            return;
        }
        publish(envelope, "store.activated");
    }

    /**
     * 门店暂停事件。
     */
    public void publishStoreSuspended(Store store) {
        EventEnvelope envelope = buildEnvelope("com.campusfruit.merchant.StoreSuspended",
                "Store", String.valueOf(store.getId()));
        try {
            envelope.setPayload(objectMapper.writeValueAsString(store));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize store payload", e);
            return;
        }
        publish(envelope, "store.suspended");
    }

    /**
     * 门店位置变更事件。
     */
    public void publishStoreLocationChanged(Store store) {
        EventEnvelope envelope = buildEnvelope("com.campusfruit.merchant.StoreLocationChanged",
                "Store", String.valueOf(store.getId()));
        try {
            envelope.setPayload(objectMapper.writeValueAsString(store));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize store payload", e);
            return;
        }
        publish(envelope, "store.location.changed");
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
            log.debug("Published event: type={}, routingKey={}, eventId={}",
                    envelope.getEventType(), routingKey, envelope.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event: type={}, routingKey={}", envelope.getEventType(), routingKey, e);
        }
    }
}
