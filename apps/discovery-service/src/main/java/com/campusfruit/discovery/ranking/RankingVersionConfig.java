package com.campusfruit.discovery.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 排名引擎版本化配置。
 */
@Component
@ConfigurationProperties(prefix = "app.ranking")
public class RankingVersionConfig {

    /** 冷启动：每页最多探索位数量 */
    private int coldStartMaxExplorationSlots = 2;

    /** 冷启动：最少评论数阈值（< 10 条为冷启动） */
    private int coldStartMinReviewCount = 10;

    /** 冷启动：最少营业天数（< 30 天为冷启动） */
    private int coldStartMinOpeningDays = 30;

    /** 冷启动：探索加分 */
    private double coldStartExplorationBonus = 0.15;

    /** 冷启动：低评分降权阈值 */
    private double coldStartMinRatingThreshold = 2.0;

    /** 冷启动：低评分最小评论数（评论数够多才降权） */
    private int coldStartLowRatingMinReviewCount = 5;

    /** Bayesian 先验平均分 */
    private double bayesianPriorMean = 3.5;

    /** Bayesian 先验权重（虚拟评论数） */
    private int bayesianPriorWeight = 10;

    // --- Getters / Setters ---

    public int getColdStartMaxExplorationSlots() { return coldStartMaxExplorationSlots; }
    public void setColdStartMaxExplorationSlots(int coldStartMaxExplorationSlots) {
        this.coldStartMaxExplorationSlots = coldStartMaxExplorationSlots;
    }

    public int getColdStartMinReviewCount() { return coldStartMinReviewCount; }
    public void setColdStartMinReviewCount(int coldStartMinReviewCount) {
        this.coldStartMinReviewCount = coldStartMinReviewCount;
    }

    public int getColdStartMinOpeningDays() { return coldStartMinOpeningDays; }
    public void setColdStartMinOpeningDays(int coldStartMinOpeningDays) {
        this.coldStartMinOpeningDays = coldStartMinOpeningDays;
    }

    public double getColdStartExplorationBonus() { return coldStartExplorationBonus; }
    public void setColdStartExplorationBonus(double coldStartExplorationBonus) {
        this.coldStartExplorationBonus = coldStartExplorationBonus;
    }

    public double getColdStartMinRatingThreshold() { return coldStartMinRatingThreshold; }
    public void setColdStartMinRatingThreshold(double coldStartMinRatingThreshold) {
        this.coldStartMinRatingThreshold = coldStartMinRatingThreshold;
    }

    public int getColdStartLowRatingMinReviewCount() { return coldStartLowRatingMinReviewCount; }
    public void setColdStartLowRatingMinReviewCount(int coldStartLowRatingMinReviewCount) {
        this.coldStartLowRatingMinReviewCount = coldStartLowRatingMinReviewCount;
    }

    public double getBayesianPriorMean() { return bayesianPriorMean; }
    public void setBayesianPriorMean(double bayesianPriorMean) { this.bayesianPriorMean = bayesianPriorMean; }

    public int getBayesianPriorWeight() { return bayesianPriorWeight; }
    public void setBayesianPriorWeight(int bayesianPriorWeight) { this.bayesianPriorWeight = bayesianPriorWeight; }
}
