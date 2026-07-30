package com.campusfruit.review.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "review_eligibilities")
public class ReviewEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "order_completed_at")
    private Instant orderCompletedAt;

    @Column(nullable = false)
    private Boolean used = false;

    @Column(nullable = false)
    private Boolean tombstone = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Instant getOrderCompletedAt() { return orderCompletedAt; }
    public void setOrderCompletedAt(Instant orderCompletedAt) { this.orderCompletedAt = orderCompletedAt; }

    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }

    public Boolean getTombstone() { return tombstone; }
    public void setTombstone(Boolean tombstone) { this.tombstone = tombstone; }

    public Instant getCreatedAt() { return createdAt; }
}
