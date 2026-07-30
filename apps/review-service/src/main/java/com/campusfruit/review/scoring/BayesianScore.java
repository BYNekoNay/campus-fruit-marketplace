package com.campusfruit.review.scoring;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 贝叶斯评分 VO。
 */
public class BayesianScore {

    /** 贝叶斯调整后评分 */
    private BigDecimal bayesianScore;

    /** 算术平均分 */
    private BigDecimal avgRating;

    /** 有效评价总数 */
    private Integer totalRatings;

    /** 置信度 (0.0 ~ 1.0)，样本越多越接近 1 */
    private Double confidence;

    /** 评分分布：key 为 1-5 星，value 为数量 */
    private Map<Integer, Long> distribution;

    public BigDecimal getBayesianScore() { return bayesianScore; }
    public void setBayesianScore(BigDecimal bayesianScore) { this.bayesianScore = bayesianScore; }

    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }

    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Map<Integer, Long> getDistribution() { return distribution; }
    public void setDistribution(Map<Integer, Long> distribution) { this.distribution = distribution; }
}
