package com.campusfruit.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 事件信封校验器。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>缺失 source/producer 字段 → 拒绝解析</li>
 *   <li>缺失 aggregate 版本（aggregateVersion {@literal <=} 0）→ 拒绝解析</li>
 *   <li>未知主版本号（当前仅支持 v1）→ 拒绝解析</li>
 * </ul>
 */
public final class EventEnvelopeValidator {

    /** 当前支持的事件主版本 */
    public static final int SUPPORTED_MAJOR_VERSION = 1;

    private EventEnvelopeValidator() {
        // 工具类禁止实例化
    }

    /**
     * 校验事件信封，返回校验错误列表。
     * 如果列表为空，表示校验通过。
     *
     * @param envelope 待校验的事件信封
     * @return 校验错误消息列表（不可变）
     */
    public static List<String> validate(EventEnvelope envelope) {
        if (envelope == null) {
            return Collections.singletonList("EventEnvelope 不能为 null");
        }

        List<String> errors = new ArrayList<>();

        // 事件 ID
        if (envelope.getEventId() == null) {
            errors.add("eventId 不能为 null");
        }

        // 事件类型
        if (envelope.getEventType() == null || envelope.getEventType().isBlank()) {
            errors.add("eventType 不能为空");
        }

        // 生产者 / source
        if (envelope.getProducer() == null || envelope.getProducer().isBlank()) {
            errors.add("producer（消息来源）不能为空，拒绝解析");
        }

        // 事件版本
        if (envelope.getEventVersion() <= 0) {
            errors.add("eventVersion 必须 > 0");
        }

        // 主版本检查
        if (envelope.getEventVersion() > SUPPORTED_MAJOR_VERSION * 100) {
            errors.add("未知主版本号: " + envelope.getEventVersion()
                    + "，当前仅支持主版本 " + SUPPORTED_MAJOR_VERSION + ".x，拒绝解析");
        }

        // 发生时间
        if (envelope.getOccurredAt() == null) {
            errors.add("occurredAt 不能为 null");
        }

        // 聚合根类型
        if (envelope.getAggregateType() == null || envelope.getAggregateType().isBlank()) {
            errors.add("aggregateType 不能为空");
        }

        // 聚合根 ID
        if (envelope.getAggregateId() == null || envelope.getAggregateId().isBlank()) {
            errors.add("aggregateId 不能为空");
        }

        // 聚合版本
        if (envelope.getAggregateVersion() <= 0) {
            errors.add("aggregateVersion 必须 > 0，拒绝解析");
        }

        // 序列号
        if (envelope.getSourceSequence() < 0) {
            errors.add("sourceSequence 不能为负数");
        }

        return Collections.unmodifiableList(errors);
    }

    /**
     * 校验事件信封，不通过时抛出 {@link IllegalArgumentException}。
     *
     * @param envelope 待校验的事件信封
     * @throws IllegalArgumentException 如果校验失败
     */
    public static void validateOrThrow(EventEnvelope envelope) {
        List<String> errors = validate(envelope);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("事件信封校验失败: " + String.join("; ", errors));
        }
    }
}
