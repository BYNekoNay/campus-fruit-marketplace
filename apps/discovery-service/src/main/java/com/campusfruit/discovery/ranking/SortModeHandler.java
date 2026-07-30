package com.campusfruit.discovery.ranking;

import com.campusfruit.discovery.client.OrderProjectionClient;
import com.campusfruit.discovery.dto.SearchRequest.SortBy;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排序模式处理器：支持6种排序方式。
 */
@Component
public class SortModeHandler {

    private static final Logger log = LoggerFactory.getLogger(SortModeHandler.class);

    private final RankingFormula rankingFormula;
    private final OrderProjectionClient orderClient;

    public SortModeHandler(RankingFormula rankingFormula, OrderProjectionClient orderClient) {
        this.rankingFormula = rankingFormula;
        this.orderClient = orderClient;
    }

    /**
     * 按指定排序模式排序。
     */
    public List<RankedOffer> sort(List<StoreOfferProjection> projections,
                                   SortBy sortBy,
                                   Double userLat, Double userLng,
                                   String keyword) {
        return switch (sortBy) {
            case COMPREHENSIVE -> sortByComprehensive(projections, userLat, userLng, keyword);
            case DISTANCE -> sortByDistance(projections, userLat, userLng);
            case PRICE_ASC -> sortByPriceAsc(projections);
            case PRICE_DESC -> sortByPriceDesc(projections);
            case RATING -> sortByRating(projections);
            case SALES -> sortBySales(projections);
        };
    }

    /**
     * 综合排序：使用 RankingFormula 加权计算。
     */
    private List<RankedOffer> sortByComprehensive(List<StoreOfferProjection> projections,
                                                   Double userLat, Double userLng,
                                                   String keyword) {
        // 计算当前结果集最低价格
        BigDecimal minPrice = projections.stream()
                .map(StoreOfferProjection::getStandardPricePer500g)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        boolean hasKeyword = keyword != null && !keyword.isBlank();

        List<RankedOffer> ranked = projections.stream()
                .map(p -> {
                    double keywordScore = hasKeyword ? computeKeywordMatch(p, keyword) : 0.5;
                    RankingResult result = rankingFormula.calculateRankingScore(
                            p, userLat, userLng, keywordScore, minPrice);
                    return new RankedOffer(p, result);
                })
                .sorted(Comparator.comparingDouble((RankedOffer r) -> r.result.getScore()).reversed())
                .collect(Collectors.toList());

        return ranked;
    }

    /**
     * 距离优先：用户有定位时按距离升序，无定位时保持原序。
     */
    private List<RankedOffer> sortByDistance(List<StoreOfferProjection> projections,
                                              Double userLat, Double userLng) {
        if (userLat == null || userLng == null) {
            return projections.stream()
                    .map(p -> new RankedOffer(p, new RankingResult(0, Map.of(), "no-location")))
                    .collect(Collectors.toList());
        }

        return projections.stream()
                .map(p -> {
                    double distanceKm = 0;
                    if (p.getStoreLat() != null && p.getStoreLng() != null) {
                        distanceKm = RankingFormula.haversineKm(userLat, userLng, p.getStoreLat(), p.getStoreLng());
                    }
                    Map<String, Double> subScores = new HashMap<>();
                    subScores.put("distanceKm", Math.round(distanceKm * 100.0) / 100.0);
                    double score = distanceKm > 0 ? 1.0 / (1.0 + distanceKm) : 0;
                    return new RankedOffer(p, new RankingResult(score, subScores));
                })
                .sorted(Comparator.comparingDouble(
                        r -> r.result.getSubScores().getOrDefault("distanceKm", Double.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    /**
     * 价格优先升序。
     */
    private List<RankedOffer> sortByPriceAsc(List<StoreOfferProjection> projections) {
        return projections.stream()
                .sorted(Comparator.comparing(
                        p -> p.getStandardPricePer500g() != null ? p.getStandardPricePer500g() : BigDecimal.valueOf(Long.MAX_VALUE)))
                .map(p -> new RankedOffer(p, new RankingResult(0, Map.of())))
                .collect(Collectors.toList());
    }

    /**
     * 价格优先降序。
     */
    private List<RankedOffer> sortByPriceDesc(List<StoreOfferProjection> projections) {
        return projections.stream()
                .sorted(Comparator.comparing(
                        (StoreOfferProjection p) -> p.getStandardPricePer500g() != null ? p.getStandardPricePer500g() : BigDecimal.ZERO)
                        .reversed())
                .map(p -> new RankedOffer(p, new RankingResult(0, Map.of())))
                .collect(Collectors.toList());
    }

    /**
     * 评分优先：按 avgRating 降序。
     */
    private List<RankedOffer> sortByRating(List<StoreOfferProjection> projections) {
        return projections.stream()
                .sorted(Comparator.comparing(
                        (StoreOfferProjection p) -> p.getAvgRating() != null ? p.getAvgRating() : BigDecimal.ZERO)
                        .reversed())
                .map(p -> new RankedOffer(p, new RankingResult(0, Map.of())))
                .collect(Collectors.toList());
    }

    /**
     * 销量优先：优先使用近30天真实销售数据，回退到 reviewCount。
     */
    private List<RankedOffer> sortBySales(List<StoreOfferProjection> projections) {
        // 尝试获取近30天真实销售数据
        Map<Long, Long> salesStats = Collections.emptyMap();
        boolean hasRealData = false;
        try {
            salesStats = orderClient.getSalesStatsPerStore();
            hasRealData = !salesStats.isEmpty();
        } catch (Exception e) {
            log.warn("Failed to fetch real sales stats, falling back to reviewCount: {}", e.getMessage());
        }

        String salesSource = hasRealData ? "基于近30天销售" : "基于评价热度（参考）";

        boolean finalHasRealData = hasRealData;
        Map<Long, Long> finalSalesStats = salesStats;

        return projections.stream()
                .map(p -> {
                    Map<String, Double> subScores = new HashMap<>();
                    double salesScore;
                    if (finalHasRealData) {
                        Long storeSales = finalSalesStats.getOrDefault(p.getStoreId(), 0L);
                        salesScore = storeSales.doubleValue();
                        subScores.put("realSales", storeSales.doubleValue());
                    } else {
                        int reviewCount = p.getReviewCount() != null ? p.getReviewCount() : 0;
                        salesScore = reviewCount;
                        subScores.put("reviewCount", (double) reviewCount);
                    }
                    subScores.put("salesSource", finalHasRealData ? 1.0 : 0.0);
                    RankingResult result = new RankingResult(salesScore, subScores);
                    return new RankedOffer(p, result);
                })
                .sorted(Comparator.comparingDouble(
                        (RankedOffer r) -> r.result.getScore()).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 简单关键词匹配度计算。
     */
    private double computeKeywordMatch(StoreOfferProjection p, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        int score = 0;
        int maxScore = 6;

        if (p.getFruitVariety() != null && p.getFruitVariety().toLowerCase().contains(lowerKeyword)) score += 2;
        if (p.getFruitCategory() != null && p.getFruitCategory().toLowerCase().contains(lowerKeyword)) score += 1;
        if (p.getFruitGrade() != null && p.getFruitGrade().toLowerCase().contains(lowerKeyword)) score += 1;
        if (p.getFruitOrigin() != null && p.getFruitOrigin().toLowerCase().contains(lowerKeyword)) score += 1;
        if (p.getStoreName() != null && p.getStoreName().toLowerCase().contains(lowerKeyword)) score += 1;

        return Math.min(1.0, (double) score / maxScore);
    }

    /**
     * 排序后的报价条目（携带排名结果）。
     */
    public static class RankedOffer {
        public final StoreOfferProjection projection;
        public final RankingResult result;

        public RankedOffer(StoreOfferProjection projection, RankingResult result) {
            this.projection = projection;
            this.result = result;
        }
    }
}
