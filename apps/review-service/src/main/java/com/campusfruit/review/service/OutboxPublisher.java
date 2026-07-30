package com.campusfruit.review.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 评价服务的发件箱发布器。
 * 每 5 秒扫描未发布的 outbox 事件并发送到 RabbitMQ。
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishOutboxEvents() {
        // 本服务当前暂不通过 outbox 发布事件，预留扩展点。
        // 未来可在此处发布 ReviewPublished、ReviewUpdated 等事件。
        log.debug("OutboxPublisher: 当前无待发送事件");
    }
}
