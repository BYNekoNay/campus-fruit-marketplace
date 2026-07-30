package com.campusfruit.offer.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class OfferResponse {

    private Long id;
    private Long storeId;
    private Long canonicalFruitId;
    private String fruitCategory;
    private String fruitVariety;
    private String fruitGrade;
    private String salesUnit;
    private Integer netWeightGrams;
    private Long unitPrice;
    private BigDecimal standardPricePer500g;
    private BigDecimal standardPricePerKg;
    private Integer stockQuantity;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private String status;
    private String qualityDesc;
    private Instant lastConfirmedAt;
    private boolean priceStale;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getCanonicalFruitId() { return canonicalFruitId; }
    public void setCanonicalFruitId(Long canonicalFruitId) { this.canonicalFruitId = canonicalFruitId; }

    public String getFruitCategory() { return fruitCategory; }
    public void setFruitCategory(String fruitCategory) { this.fruitCategory = fruitCategory; }

    public String getFruitVariety() { return fruitVariety; }
    public void setFruitVariety(String fruitVariety) { this.fruitVariety = fruitVariety; }

    public String getFruitGrade() { return fruitGrade; }
    public void setFruitGrade(String fruitGrade) { this.fruitGrade = fruitGrade; }

    public String getSalesUnit() { return salesUnit; }
    public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }

    public Integer getNetWeightGrams() { return netWeightGrams; }
    public void setNetWeightGrams(Integer netWeightGrams) { this.netWeightGrams = netWeightGrams; }

    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getStandardPricePer500g() { return standardPricePer500g; }
    public void setStandardPricePer500g(BigDecimal standardPricePer500g) { this.standardPricePer500g = standardPricePer500g; }

    public BigDecimal getStandardPricePerKg() { return standardPricePerKg; }
    public void setStandardPricePerKg(BigDecimal standardPricePerKg) { this.standardPricePerKg = standardPricePerKg; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQualityDesc() { return qualityDesc; }
    public void setQualityDesc(String qualityDesc) { this.qualityDesc = qualityDesc; }

    public Instant getLastConfirmedAt() { return lastConfirmedAt; }
    public void setLastConfirmedAt(Instant lastConfirmedAt) { this.lastConfirmedAt = lastConfirmedAt; }

    public boolean isPriceStale() { return priceStale; }
    public void setPriceStale(boolean priceStale) { this.priceStale = priceStale; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
