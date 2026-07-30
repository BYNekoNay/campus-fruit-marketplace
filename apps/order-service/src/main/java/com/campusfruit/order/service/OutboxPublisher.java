package com.campusfruit.order.service;

import com.campusfruit.order.entity.OutboxEvent;
import com.campusfruit.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 发件箱发布器。
 * <p>
 * 每 5 秒扫描未发布的 outbox 事件，发送到 RabbitMQ 并标记为已发布。
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                            RabbitTemplate rabbitTemplate,
                            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();
        if (unpublished.isEmpty()) {
            return;
        }

        log.debug("扫描到 {} 条未发布的 outbox 事件", unpublished.size());

        for (OutboxEvent event : unpublished) {
            try {
                String routingKey = event.getEventType().replace('.', '.');
                rabbitTemplate.convertAndSend("campus-fruit.events", routingKey, event.getPayload());
                event.setPublished(true);
                outboxEventRepository.save(event);

                log.info("发布 outbox 事件: id={}, type={}, aggregateId={}",
                        event.getId(), event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("发布 outbox 事件失败: id={}, type={}, error={}",
                        event.getId(), event.getEventType(), e.getMessage());
            }
        }
    }
}
