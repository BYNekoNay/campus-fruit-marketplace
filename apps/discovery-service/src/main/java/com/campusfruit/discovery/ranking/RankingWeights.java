package com.campusfruit.discovery.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ranking.weights")
public class RankingWeights {

    /** 相关性权重（关键词匹配度） */
    private double relevance = 0.30;

    /** 价格权重 */
    private double price = 0.25;

    /** 距离权重 */
    private double distance = 0.20;

    /** 评分权重 */
    private double rating = 0.15;

    /** 履约权重（门店状态） */
    private double fulfillment = 0.10;

    public double getRelevance() { return relevance; }
    public void setRelevance(double relevance) { this.relevance = relevance; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public double getFulfillment() { return fulfillment; }
    public void setFulfillment(double fulfillment) { this.fulfillment = fulfillment; }

    /**
     * 判断权重之和是否有效（接近 1.0）。
     */
    public boolean isValid() {
        double sum = relevance + price + distance + rating + fulfillment;
        return Math.abs(sum - 1.0) < 0.001;
    }

    /**
     * 重新分配距离权重到其他维度（无用户定位时使用）。
     */
    public double[] redistributeDistanceWeight() {
        double remaining = 1.0 - distance;
        if (remaining <= 0) {
            return new double[]{0.25, 0.25, 0.25, 0.25};
        }
        return new double[]{
                relevance / remaining,
                price / remaining,
                rating / remaining,
                fulfillment / remaining
        };
    }
}
