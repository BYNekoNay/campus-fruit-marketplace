package com.campusfruit.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import java.time.Instant;
import java.util.UUID;

/**
 * 事件信封 —— 所有领域事件的标准化包装结构。
 * <p>
 * 设计遵循 CloudEvents 核心字段 + DDD 聚合版本追踪。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope {

    /** 事件唯一标识 */
    private UUID eventId;

    /** 事件类型（全限定名，如 com.campusfruit.order.OrderPlaced） */
    private String eventType;

    /** 事件版本号，用于 Schema 演化 */
    private int eventVersion = 1;

    /** 事件发生时间 */
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant occurredAt;

    /** 生产者服务名 */
    private String producer;

    /** 生产者侧递增序号 */
    private long sourceSequence;

    /** 分布式追踪 ID */
    private String traceId;

    /** 聚合根类型 */
    private String aggregateType;

    /** 聚合根 ID */
    private String aggregateId;

    /** 聚合版本号（乐观锁/事件溯源） */
    private long aggregateVersion;

    /** 事件载荷 JSON 字符串 */
    private String payload;

    /** 是否为墓碑事件（逻辑删除标记） */
    private boolean tombstone;

    // --- 构造器 ---

    public EventEnvelope() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    // --- Getters / Setters ---

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(int eventVersion) {
        this.eventVersion = eventVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public long getSourceSequence() {
        return sourceSequence;
    }

    public void setSourceSequence(long sourceSequence) {
        this.sourceSequence = sourceSequence;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public long getAggregateVersion() {
        return aggregateVersion;
    }

    public void setAggregateVersion(long aggregateVersion) {
        this.aggregateVersion = aggregateVersion;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public boolean isTombstone() {
        return tombstone;
    }

    public void setTombstone(boolean tombstone) {
        this.tombstone = tombstone;
    }

    @Override
    public String toString() {
        return "EventEnvelope{" +
                "eventId=" + eventId +
                ", eventType='" + eventType + '\'' +
                ", eventVersion=" + eventVersion +
                ", aggregateType='" + aggregateType + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", aggregateVersion=" + aggregateVersion +
                ", tombstone=" + tombstone +
                '}';
    }
}
