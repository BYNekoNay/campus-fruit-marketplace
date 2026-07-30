package com.campusfruit.discovery.ranking;

import com.campusfruit.discovery.entity.StoreOfferProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 综合排序加权公式。
 */
@Component
public class RankingFormula {

    private final RankingWeights weights;
    private final RankingVersionConfig versionConfig;

    public RankingFormula(RankingWeights weights, RankingVersionConfig versionConfig) {
        this.weights = weights;
        this.versionConfig = versionConfig;
    }

    /**
     * 计算综合排序得分。
     *
     * @param p       报价投影
     * @param userLat  用户纬度（可为 null）
     * @param userLng  用户经度（可为 null）
     * @param keywordMatchScore 关键词匹配度（0-1），无关键词时传 0.5
     * @param minPriceInResults 当前结果集最低价格（用于归一化价格得分）
     * @return 排序结果
     */
    public RankingResult calculateRankingScore(StoreOfferProjection p,
                                                Double userLat, Double userLng,
                                                double keywordMatchScore,
                                                BigDecimal minPriceInResults) {
        boolean hasLocation = userLat != null && userLng != null
                && p.getStoreLat() != null && p.getStoreLng() != null;

        // 1. 相关性得分：关键词匹配度（0-1）
        double relevanceScore = clamp(keywordMatchScore, 0.0, 1.0);

        // 2. 价格得分：minPrice / offerPrice（价格越低分数越高）
        double priceScore = computePriceScore(p.getStandardPricePer500g(), minPriceInResults);

        // 3. 距离得分：1 / (1 + distanceKm)
        double distanceScore = 0.0;
        double distanceKm = 0.0;
        if (hasLocation) {
            distanceKm = haversineKm(userLat, userLng, p.getStoreLat(), p.getStoreLng());
            distanceScore = 1.0 / (1.0 + distanceKm);
        }

        // 4. 评分得分：bayesian平滑后的评分 / 5.0
        double ratingScore = computeBayesianRatingScore(
                p.getAvgRating() != null ? p.getAvgRating().doubleValue() : 0.0,
                p.getReviewCount() != null ? p.getReviewCount() : 0);

        // 5. 履约得分：OPEN=1, 其他=0
        double fulfillmentScore = "OPEN".equalsIgnoreCase(p.getStoreStatus()) ? 1.0 : 0.0;

        // 权重
        double wRelevance = weights.getRelevance();
        double wPrice = weights.getPrice();
        double wDistance = weights.getDistance();
        double wRating = weights.getRating();
        double wFulfillment = weights.getFulfillment();

        // 无用户定位时，距离权重重分配到其他维度
        if (!hasLocation) {
            double[] redist = weights.redistributeDistanceWeight();
            wRelevance = redist[0] * (weights.getRelevance() / (1.0 - weights.getDistance()));
            wPrice = redist[1] * (weights.getPrice() / (1.0 - weights.getDistance()));
            wRating = redist[2] * (weights.getRating() / (1.0 - weights.getDistance()));
            wFulfillment = redist[3] * (weights.getFulfillment() / (1.0 - weights.getDistance()));
            wDistance = 0.0;
        }

        double score = wRelevance * relevanceScore
                + wPrice * priceScore
                + wDistance * distanceScore
                + wRating * ratingScore
                + wFulfillment * fulfillmentScore;

        Map<String, Double> subScores = new HashMap<>();
        subScores.put("relevance", relevanceScore);
        subScores.put("price", priceScore);
        subScores.put("distance", distanceScore);
        subScores.put("rating", ratingScore);
        subScores.put("fulfillment", fulfillmentScore);
        subScores.put("distanceKm", Math.round(distanceKm * 100.0) / 100.0);

        return new RankingResult(score, subScores);
    }

    /**
     * 价格得分：最低价/当前价（0.05-1.0 范围内钳制）。
     */
    private double computePriceScore(BigDecimal offerPrice, BigDecimal minPrice) {
        if (offerPrice == null || minPrice == null || minPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.5; // 默认中等得分
        }
        double ratio = minPrice.divide(offerPrice, 4, RoundingMode.HALF_UP).doubleValue();
        return clamp(ratio, 0.05, 1.0);
    }

    /**
     * Bayesian 平滑评分
     */
    private double computeBayesianRatingScore(double avgRating, int reviewCount) {
        double priorMean = versionConfig.getBayesianPriorMean();
        int priorWeight = versionConfig.getBayesianPriorWeight();
        double smoothed = (avgRating * reviewCount + priorMean * priorWeight) / (reviewCount + priorWeight);
        return clamp(smoothed / 5.0, 0.0, 1.0);
    }

    /**
     * Haversine 公式计算两坐标距离（km）。
     */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
