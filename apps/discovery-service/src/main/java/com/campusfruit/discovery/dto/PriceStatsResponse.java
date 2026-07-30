package com.campusfruit.discovery.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceStatsResponse {

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal medianPrice;
    private BigDecimal avgPrice;
    private Integer storeCount;
    private Integer sampleCount;
    private LocalDate sampleTime;
    private boolean sampleInsufficient;

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

    public LocalDate getSampleTime() { return sampleTime; }
    public void setSampleTime(LocalDate sampleTime) { this.sampleTime = sampleTime; }

    public boolean isSampleInsufficient() { return sampleInsufficient; }
    public void setSampleInsufficient(boolean sampleInsufficient) { this.sampleInsufficient = sampleInsufficient; }
}
