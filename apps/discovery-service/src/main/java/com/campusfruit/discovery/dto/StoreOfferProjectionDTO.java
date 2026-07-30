package com.campusfruit.discovery.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class StoreOfferProjectionDTO {

    private Long storeId;
    private String storeName;
    private Double storeLat;
    private Double storeLng;
    private Double distance;
    private Long offerId;
    private String fruitVariety;
    private String fruitGrade;
    private String salesUnit;
    private BigDecimal unitPriceYuan;
    private BigDecimal standardPricePer500g;
    private Boolean isComparable;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Boolean priceStale;
    private Double rankingScore;
    private String rankingTraceId;
    private Boolean coldStart;
    private String salesSource;

    public StoreOfferProjectionDTO() {
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public Double getStoreLat() { return storeLat; }
    public void setStoreLat(Double storeLat) { this.storeLat = storeLat; }

    public Double getStoreLng() { return storeLng; }
    public void setStoreLng(Double storeLng) { this.storeLng = storeLng; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) {
        this.distance = distance != null ? round2(distance) : null;
    }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public String getFruitVariety() { return fruitVariety; }
    public void setFruitVariety(String fruitVariety) { this.fruitVariety = fruitVariety; }

    public String getFruitGrade() { return fruitGrade; }
    public void setFruitGrade(String fruitGrade) { this.fruitGrade = fruitGrade; }

    public String getSalesUnit() { return salesUnit; }
    public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }

    public BigDecimal getUnitPriceYuan() { return unitPriceYuan; }
    public void setUnitPriceYuan(BigDecimal unitPriceYuan) { this.unitPriceYuan = unitPriceYuan; }

    public BigDecimal getStandardPricePer500g() { return standardPricePer500g; }
    public void setStandardPricePer500g(BigDecimal standardPricePer500g) { this.standardPricePer500g = standardPricePer500g; }

    public Boolean getIsComparable() { return isComparable; }
    public void setIsComparable(Boolean isComparable) { this.isComparable = isComparable; }

    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public Boolean getPriceStale() { return priceStale; }
    public void setPriceStale(Boolean priceStale) { this.priceStale = priceStale; }

    public Double getRankingScore() { return rankingScore; }
    public void setRankingScore(Double rankingScore) { this.rankingScore = rankingScore; }

    public String getRankingTraceId() { return rankingTraceId; }
    public void setRankingTraceId(String rankingTraceId) { this.rankingTraceId = rankingTraceId; }

    public Boolean getColdStart() { return coldStart; }
    public void setColdStart(Boolean coldStart) { this.coldStart = coldStart; }

    public String getSalesSource() { return salesSource; }
    public void setSalesSource(String salesSource) { this.salesSource = salesSource; }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
