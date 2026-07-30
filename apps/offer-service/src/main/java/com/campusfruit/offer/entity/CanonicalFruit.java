package com.campusfruit.offer.entity;

import com.campusfruit.offer.enums.FruitStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "canonical_fruits")
public class CanonicalFruit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 200)
    private String variety;

    @Column(nullable = false, length = 50)
    private String grade;

    @Column(length = 200)
    private String origin;

    @Column(name = "default_unit", length = 20)
    private String defaultUnit = "g";

    @Column(name = "comparison_group_id")
    private Long comparisonGroupId;

    @Column
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FruitStatus status = FruitStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = FruitStatus.ACTIVE;
        }
        if (this.version == null) {
            this.version = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDefaultUnit() { return defaultUnit; }
    public void setDefaultUnit(String defaultUnit) { this.defaultUnit = defaultUnit; }

    public Long getComparisonGroupId() { return comparisonGroupId; }
    public void setComparisonGroupId(Long comparisonGroupId) { this.comparisonGroupId = comparisonGroupId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public FruitStatus getStatus() { return status; }
    public void setStatus(FruitStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
