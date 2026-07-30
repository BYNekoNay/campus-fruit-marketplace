package com.campusfruit.review.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ReviewListResponse {

    private List<ReviewResponse> items;
    private BigDecimal avgRating;
    private BigDecimal bayesianRating;
    private Integer totalRatings;
    private Map<Integer, Integer> distribution;

    public List<ReviewResponse> getItems() { return items; }
    public void setItems(List<ReviewResponse> items) { this.items = items; }

    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }

    public BigDecimal getBayesianRating() { return bayesianRating; }
    public void setBayesianRating(BigDecimal bayesianRating) { this.bayesianRating = bayesianRating; }

    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }

    public Map<Integer, Integer> getDistribution() { return distribution; }
    public void setDistribution(Map<Integer, Integer> distribution) { this.distribution = distribution; }
}
