package com.campusfruit.discovery.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_daily_stats")
public class PriceDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_fruit_id", nullable = false)
    private Long canonicalFruitId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "min_price", precision = 12, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 12, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "median_price", precision = 12, scale = 2)
    private BigDecimal medianPrice;

    @Column(name = "avg_price", precision = 12, scale = 2)
    private BigDecimal avgPrice;

    @Column(name = "store_count")
    private Integer storeCount = 0;

    @Column(name = "sample_count")
    private Integer sampleCount = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCanonicalFruitId() { return canonicalFruitId; }
    public void setCanonicalFruitId(Long canonicalFruitId) { this.canonicalFruitId = canonicalFruitId; }

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public BigDecimal getMedianPrice() { return medianPrice; }
    public void setMedianPrice(BigDecimal medianPrice) { this.medianPrice = medianPrice; }

    public BigDecimal getAvgPrice() { return avgPrice; }
    public void setAvgPrice(BigDecimal avgPrice) { this.avgPrice = avgPrice; }

    public Integer getStoreCount() { return storeCount; }
    public void setStoreCount(Integer storeCount) { this.storeCount = storeCount; }

    public Integer getSampleCount() { return sampleCount; }
    public void setSampleCount(Integer sampleCount) { this.sampleCount = sampleCount; }
}
