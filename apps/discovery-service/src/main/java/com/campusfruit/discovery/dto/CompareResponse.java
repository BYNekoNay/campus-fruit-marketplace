package com.campusfruit.discovery.dto;

import java.util.List;

public class CompareResponse {

    private List<CompareItem> offers;
    private PriceStatsResponse stats;

    public CompareResponse() {
    }

    public CompareResponse(List<CompareItem> offers, PriceStatsResponse stats) {
        this.offers = offers;
        this.stats = stats;
    }

    public List<CompareItem> getOffers() { return offers; }
    public void setOffers(List<CompareItem> offers) { this.offers = offers; }

    public PriceStatsResponse getStats() { return stats; }
    public void setStats(PriceStatsResponse stats) { this.stats = stats; }

    public static class CompareItem {
        private Long offerId;
        private String storeName;
        private String fruitVariety;
        private String fruitGrade;
        private String fruitOrigin;
        private String salesUnit;
        private java.math.BigDecimal unitPriceYuan;
        private java.math.BigDecimal standardPricePer500g;
        private Boolean isComparable;
        private java.math.BigDecimal avgRating;
        private Integer reviewCount;
        private Boolean priceStale;

        public Long getOfferId() { return offerId; }
        public void setOfferId(Long offerId) { this.offerId = offerId; }

        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }

        public String getFruitVariety() { return fruitVariety; }
        public void setFruitVariety(String fruitVariety) { this.fruitVariety = fruitVariety; }

        public String getFruitGrade() { return fruitGrade; }
        public void setFruitGrade(String fruitGrade) { this.fruitGrade = fruitGrade; }

        public String getFruitOrigin() { return fruitOrigin; }
        public void setFruitOrigin(String fruitOrigin) { this.fruitOrigin = fruitOrigin; }

        public String getSalesUnit() { return salesUnit; }
        public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }

        public java.math.BigDecimal getUnitPriceYuan() { return unitPriceYuan; }
        public void setUnitPriceYuan(java.math.BigDecimal unitPriceYuan) { this.unitPriceYuan = unitPriceYuan; }

        public java.math.BigDecimal getStandardPricePer500g() { return standardPricePer500g; }
        public void setStandardPricePer500g(java.math.BigDecimal standardPricePer500g) { this.standardPricePer500g = standardPricePer500g; }

        public Boolean getIsComparable() { return isComparable; }
        public void setIsComparable(Boolean isComparable) { this.isComparable = isComparable; }

        public java.math.BigDecimal getAvgRating() { return avgRating; }
        public void setAvgRating(java.math.BigDecimal avgRating) { this.avgRating = avgRating; }

        public Integer getReviewCount() { return reviewCount; }
        public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

        public Boolean getPriceStale() { return priceStale; }
        public void setPriceStale(Boolean priceStale) { this.priceStale = priceStale; }
    }
}
