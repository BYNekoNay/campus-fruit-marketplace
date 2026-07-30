package com.campusfruit.discovery.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "favorites")
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Favorite() {
    }

    public Favorite(Long userId, Long storeId) {
        this.userId = userId;
        this.storeId = storeId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Instant getCreatedAt() { return createdAt; }
}
