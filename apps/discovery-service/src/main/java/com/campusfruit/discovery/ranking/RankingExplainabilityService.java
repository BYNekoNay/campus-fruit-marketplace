package com.campusfruit.discovery.ranking;

import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 排序解释性服务：提供排序结果的人性化中文解释。
 */
@Service
public class RankingExplainabilityService {

    private static final String ALGORITHM_VERSION = "v1.0.0";

    private final StoreOfferProjectionRepository projectionRepository;
    private final ColdStartHandler coldStartHandler;

    /** traceId -> RankingResult 缓存 */
    private final Map<String, RankingResult> traceCache = new ConcurrentHashMap<>();

    public RankingExplainabilityService(StoreOfferProjectionRepository projectionRepository,
                                         ColdStartHandler coldStartHandler) {
        this.projectionRepository = projectionRepository;
        this.coldStartHandler = coldStartHandler;
    }

    /**
     * 缓存排序结果以供后续解释。
     */
    public void cacheTrace(RankingResult result) {
        if (result != null && result.getRankingTraceId() != null) {
            traceCache.put(result.getRankingTraceId(), result);
        }
    }

    /**
     * 根据 traceId 获取排序解释。
     */
    public RankingExplanation explain(Long offerId, String rankingTraceId) {
        RankingResult rankingResult = traceCache.get(rankingTraceId);
        if (rankingResult == null) {
            return fallbackExplanation(offerId);
        }

        RankingExplanation explanation = new RankingExplanation();
        explanation.setOverallScore(rankingResult.getScore());
        explanation.setSubScores(rankingResult.getSubScores());
        explanation.setAlgorithmVersion(ALGORITHM_VERSION);

        StoreOfferProjection projection = projectionRepository.findByOfferId(offerId).orElse(null);
        if (projection == null) {
            explanation.setRankingReason("无相关信息");
            return explanation;
        }

        boolean isColdStart = coldStartHandler.isNewStore(projection);
        explanation.setColdStart(isColdStart);

        if (isColdStart) {
            explanation.setRankingReason("新店推荐");
        } else {
            explanation.setRankingReason(generateReason(projection, rankingResult));
        }

        return explanation;
    }

    private String generateReason(StoreOfferProjection p, RankingResult result) {
        Map<String, Double> subs = result.getSubScores();
        StringBuilder sb = new StringBuilder();

        // 价格优势优先
        double priceScore = subs.getOrDefault("price", 0.0);
        if (priceScore > 0.85 && p.getStandardPricePer500g() != null) {
            sb.append("价格实惠，").append(p.getStandardPricePer500g().setScale(1, RoundingMode.HALF_UP))
                    .append("元/500g");
            return sb.toString();
        }

        // 距离优势
        double distanceKm = subs.getOrDefault("distanceKm", Double.MAX_VALUE);
        if (distanceKm > 0 && distanceKm < 2.0) {
            sb.append("距离最近，仅").append(String.format("%.1f", distanceKm)).append("km");
            return sb.toString();
        }

        // 评分优势
        double ratingScore = subs.getOrDefault("rating", 0.0);
        if (p.getAvgRating() != null && p.getAvgRating().doubleValue() >= 4.5) {
            sb.append("评分最高").append(p.getAvgRating().setScale(1, RoundingMode.HALF_UP)).append("分");
            return sb.toString();
        }

        // 综合推荐
        if (result.getScore() > 0.7) {
            return "综合评分高，推荐品质之选";
        }

        return "综合排序结果";
    }

    private RankingExplanation fallbackExplanation(Long offerId) {
        RankingExplanation explanation = new RankingExplanation();
        explanation.setOverallScore(0.0);
        explanation.setAlgorithmVersion(ALGORITHM_VERSION);
        explanation.setRankingReason("排序数据已过期，请重新搜索");
        return explanation;
    }
}
