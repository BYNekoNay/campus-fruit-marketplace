package com.campusfruit.review.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "rating_aggregates")
public class RatingAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false, unique = true)
    private Long storeId;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "bayesian_rating", precision = 3, scale = 2)
    private BigDecimal bayesianRating;

    @Column(name = "total_ratings", nullable = false)
    private Integer totalRatings = 0;

    @Column(name = "rating_distribution", length = 200)
    private String ratingDistribution;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "calculated_at")
    private Instant calculatedAt;

    @PrePersist
    protected void onCreate() {
        this.calculatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }

    public BigDecimal getBayesianRating() { return bayesianRating; }
    public void setBayesianRating(BigDecimal bayesianRating) { this.bayesianRating = bayesianRating; }

    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }

    public String getRatingDistribution() { return ratingDistribution; }
    public void setRatingDistribution(String ratingDistribution) { this.ratingDistribution = ratingDistribution; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}
