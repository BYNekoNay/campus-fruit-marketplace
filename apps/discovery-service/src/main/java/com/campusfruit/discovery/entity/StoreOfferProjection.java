package com.campusfruit.discovery.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "store_offer_projections")
public class StoreOfferProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "store_name", nullable = false, length = 200)
    private String storeName;

    @Column(name = "store_address", columnDefinition = "TEXT")
    private String storeAddress;

    @Column(name = "store_lat")
    private Double storeLat;

    @Column(name = "store_lng")
    private Double storeLng;

    @Column(name = "store_phone", length = 20)
    private String storePhone;

    @Column(name = "store_status", length = 20)
    private String storeStatus;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "merchant_status", length = 20)
    private String merchantStatus = "APPROVED";

    @Column(name = "canonical_fruit_id")
    private Long canonicalFruitId;

    @Column(name = "fruit_category", length = 100)
    private String fruitCategory;

    @Column(name = "fruit_variety", length = 200)
    private String fruitVariety;

    @Column(name = "fruit_grade", length = 50)
    private String fruitGrade;

    @Column(name = "fruit_origin", length = 200)
    private String fruitOrigin;

    @Column(name = "sales_unit", length = 50)
    private String salesUnit;

    @Column(name = "net_weight_grams")
    private Integer netWeightGrams;

    @Column(name = "unit_price")
    private Long unitPrice;

    @Column(name = "standard_price_per500g", precision = 12, scale = 2)
    private BigDecimal standardPricePer500g;

    @Column(name = "is_comparable")
    private Boolean isComparable = true;

    @Column(name = "available_quantity")
    private Integer availableQuantity = 0;

    @Column(name = "offer_status", length = 20)
    private String offerStatus;

    @Column(name = "price_stale")
    private Boolean priceStale = false;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "aggregate_version")
    private Integer aggregateVersion = 1;

    @Column(name = "last_event_type", length = 100)
    private String lastEventType;

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getStoreAddress() { return storeAddress; }
    public void setStoreAddress(String storeAddress) { this.storeAddress = storeAddress; }

    public Double getStoreLat() { return storeLat; }
    public void setStoreLat(Double storeLat) { this.storeLat = storeLat; }

    public Double getStoreLng() { return storeLng; }
    public void setStoreLng(Double storeLng) { this.storeLng = storeLng; }

    public String getStorePhone() { return storePhone; }
    public void setStorePhone(String storePhone) { this.storePhone = storePhone; }

    public String getStoreStatus() { return storeStatus; }
    public void setStoreStatus(String storeStatus) { this.storeStatus = storeStatus; }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getMerchantStatus() { return merchantStatus; }
    public void setMerchantStatus(String merchantStatus) { this.merchantStatus = merchantStatus; }

    public Long getCanonicalFruitId() { return canonicalFruitId; }
    public void setCanonicalFruitId(Long canonicalFruitId) { this.canonicalFruitId = canonicalFruitId; }

    public String getFruitCategory() { return fruitCategory; }
    public void setFruitCategory(String fruitCategory) { this.fruitCategory = fruitCategory; }

    public String getFruitVariety() { return fruitVariety; }
    public void setFruitVariety(String fruitVariety) { this.fruitVariety = fruitVariety; }

    public String getFruitGrade() { return fruitGrade; }
    public void setFruitGrade(String fruitGrade) { this.fruitGrade = fruitGrade; }

    public String getFruitOrigin() { return fruitOrigin; }
    public void setFruitOrigin(String fruitOrigin) { this.fruitOrigin = fruitOrigin; }

    public String getSalesUnit() { return salesUnit; }
    public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }

    public Integer getNetWeightGrams() { return netWeightGrams; }
    public void setNetWeightGrams(Integer netWeightGrams) { this.netWeightGrams = netWeightGrams; }

    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getStandardPricePer500g() { return standardPricePer500g; }
    public void setStandardPricePer500g(BigDecimal standardPricePer500g) { this.standardPricePer500g = standardPricePer500g; }

    public Boolean getIsComparable() { return isComparable; }
    public void setIsComparable(Boolean isComparable) { this.isComparable = isComparable; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public String getOfferStatus() { return offerStatus; }
    public void setOfferStatus(String offerStatus) { this.offerStatus = offerStatus; }

    public Boolean getPriceStale() { return priceStale; }
    public void setPriceStale(Boolean priceStale) { this.priceStale = priceStale; }

    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public Integer getAggregateVersion() { return aggregateVersion; }
    public void setAggregateVersion(Integer aggregateVersion) { this.aggregateVersion = aggregateVersion; }

    public String getLastEventType() { return lastEventType; }
    public void setLastEventType(String lastEventType) { this.lastEventType = lastEventType; }

    public Instant getLastEventAt() { return lastEventAt; }
    public void setLastEventAt(Instant lastEventAt) { this.lastEventAt = lastEventAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
