package com.campusfruit.review.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 风控规则配置。
 * 从 application.yml 中 app.review.risk 节点读取。
 */
@Component
@ConfigurationProperties("app.review.risk")
public class RiskRuleConfig {

    /** 可疑频率检测窗口（分钟），默认 10 */
    private int suspiciousWindowMinutes = 10;

    /** 窗口内最大评价数，超过即标记 SUSPICIOUS_FREQUENCY */
    private int suspiciousMaxReviews = 5;

    /** 最小评价时间（秒），订单完成后低于此时间评价标记 SUSPICIOUS_TIMING */
    private int minReviewTimeSeconds = 30;

    /** 内容重复相似度阈值（0~1），默认 0.8 */
    private double duplicateSimilarityThreshold = 0.8;

    /** 评价激增检测窗口（分钟），默认 60 */
    private int spikeWindowMinutes = 60;

    /** 窗口内评价数阈值，超过即标记 REVIEW_SPIKE */
    private int spikeThreshold = 10;

    // --- getters / setters ---

    public int getSuspiciousWindowMinutes() { return suspiciousWindowMinutes; }
    public void setSuspiciousWindowMinutes(int suspiciousWindowMinutes) {
        this.suspiciousWindowMinutes = suspiciousWindowMinutes;
    }

    public int getSuspiciousMaxReviews() { return suspiciousMaxReviews; }
    public void setSuspiciousMaxReviews(int suspiciousMaxReviews) {
        this.suspiciousMaxReviews = suspiciousMaxReviews;
    }

    public int getMinReviewTimeSeconds() { return minReviewTimeSeconds; }
    public void setMinReviewTimeSeconds(int minReviewTimeSeconds) {
        this.minReviewTimeSeconds = minReviewTimeSeconds;
    }

    public double getDuplicateSimilarityThreshold() { return duplicateSimilarityThreshold; }
    public void setDuplicateSimilarityThreshold(double duplicateSimilarityThreshold) {
        this.duplicateSimilarityThreshold = duplicateSimilarityThreshold;
    }

    public int getSpikeWindowMinutes() { return spikeWindowMinutes; }
    public void setSpikeWindowMinutes(int spikeWindowMinutes) {
        this.spikeWindowMinutes = spikeWindowMinutes;
    }

    public int getSpikeThreshold() { return spikeThreshold; }
    public void setSpikeThreshold(int spikeThreshold) {
        this.spikeThreshold = spikeThreshold;
    }
}
