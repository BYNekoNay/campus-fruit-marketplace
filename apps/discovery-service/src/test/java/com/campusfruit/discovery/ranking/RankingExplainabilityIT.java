package com.campusfruit.discovery.ranking;

import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 集成测试：验证排序解释为每个结果生成有意义的中文解释。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Ranking explainability integration tests")
class RankingExplainabilityIT {

    @Mock
    private StoreOfferProjectionRepository projectionRepository;

    @Mock
    private RankingVersionConfig config;

    private RankingWeights weights;
    private RankingFormula formula;
    private ColdStartHandler coldStartHandler;
    private RankingExplainabilityService service;

    @BeforeEach
    void setUp() {
        weights = new RankingWeights();
        weights.setRelevance(0.30);
        weights.setPrice(0.25);
        weights.setDistance(0.20);
        weights.setRating(0.15);
        weights.setFulfillment(0.10);

        when(config.getColdStartMinReviewCount()).thenReturn(3);
        when(config.getColdStartExplorationBonus()).thenReturn(0.15);
        when(config.getColdStartMaxExplorationSlots()).thenReturn(2);
        when(config.getBayesianPriorMean()).thenReturn(3.5);
        when(config.getBayesianPriorWeight()).thenReturn(10);
        when(config.getColdStartMinRatingThreshold()).thenReturn(2.0);
        when(config.getColdStartLowRatingMinReviewCount()).thenReturn(5);

        formula = new RankingFormula(weights, config);
        coldStartHandler = new ColdStartHandler(config);
        service = new RankingExplainabilityService(projectionRepository, coldStartHandler);
    }

    @Test
    @DisplayName("Should generate explanation for new store")
    void shouldExplainNewStore() {
        StoreOfferProjection p = createProjection(1L, 1L, "OPEN", new BigDecimal("10.00"),
                new BigDecimal("4.0"), 0, 30.93, 121.46);

        when(projectionRepository.findByOfferId(1L)).thenReturn(Optional.of(p));

        RankingResult result = formula.calculateRankingScore(p, 30.9244, 121.4595, 0.8, new BigDecimal("8.00"));
        service.cacheTrace(result);

        RankingExplanation explanation = service.explain(1L, result.getRankingTraceId());

        assertNotNull(explanation);
        assertEquals("新店推荐", explanation.getRankingReason());
        assertTrue(explanation.isColdStart());
        assertEquals("v1.0.0", explanation.getAlgorithmVersion());
    }

    @Test
    @DisplayName("Should generate explanation for cheap price offer")
    void shouldExplainCheapPrice() {
        StoreOfferProjection p = createProjection(2L, 2L, "OPEN", new BigDecimal("8.00"),
                new BigDecimal("4.5"), 100, 30.935, 121.46);

        when(projectionRepository.findByOfferId(2L)).thenReturn(Optional.of(p));

        RankingResult result = formula.calculateRankingScore(p, 30.9244, 121.4595, 0.8, new BigDecimal("8.00"));
        service.cacheTrace(result);

        RankingExplanation explanation = service.explain(2L, result.getRankingTraceId());

        assertNotNull(explanation);
        assertTrue(explanation.getRankingReason().contains("实惠"),
                "价格实惠的报价应包含 '实惠' 关键词");
    }

    @Test
    @DisplayName("Should generate explanation for close distance offer")
    void shouldExplainCloseDistance() {
        StoreOfferProjection p = createProjection(3L, 3L, "OPEN", new BigDecimal("12.00"),
                new BigDecimal("4.0"), 50, 30.9250, 121.46);

        when(projectionRepository.findByOfferId(3L)).thenReturn(Optional.of(p));

        RankingResult result = formula.calculateRankingScore(p, 30.9244, 121.4595, 0.7, new BigDecimal("10.00"));
        service.cacheTrace(result);

        RankingExplanation explanation = service.explain(3L, result.getRankingTraceId());

        assertNotNull(explanation);
        assertTrue(explanation.getRankingReason().contains("最近")
                        || explanation.getRankingReason().contains("实惠"),
                "近距离报价应包含距离或价格相关解释");
    }

    @Test
    @DisplayName("Should generate explanation for high rated offer")
    void shouldExplainHighRated() {
        StoreOfferProjection p = createProjection(4L, 4L, "OPEN", new BigDecimal("15.00"),
                new BigDecimal("4.8"), 200, 30.93, 121.47);

        when(projectionRepository.findByOfferId(4L)).thenReturn(Optional.of(p));

        RankingResult result = formula.calculateRankingScore(p, 30.9244, 121.4595, 0.6, new BigDecimal("12.00"));
        service.cacheTrace(result);

        RankingExplanation explanation = service.explain(4L, result.getRankingTraceId());

        assertNotNull(explanation);
        assertTrue(explanation.getRankingReason().contains("分") || explanation.getRankingReason().contains("评分"),
                "高评分报价应包含评分相关解释");
    }

    @Test
    @DisplayName("Should return fallback for missing trace")
    void shouldReturnFallbackForMissingTrace() {
        StoreOfferProjection p = createProjection(5L, 5L, "OPEN", new BigDecimal("10.00"),
                new BigDecimal("4.0"), 10, 30.93, 121.46);

        when(projectionRepository.findByOfferId(5L)).thenReturn(Optional.of(p));

        RankingExplanation explanation = service.explain(5L, "nonexistent-trace");

        assertNotNull(explanation);
        assertEquals("排序数据已过期，请重新搜索", explanation.getRankingReason());
    }

    @Test
    @DisplayName("Sub-scores should contain all dimensions")
    void subScoresShouldContainAllDimensions() {
        StoreOfferProjection p = createProjection(6L, 6L, "OPEN", new BigDecimal("10.00"),
                new BigDecimal("4.5"), 50, 30.93, 121.46);

        RankingResult result = formula.calculateRankingScore(p, 30.9244, 121.4595, 0.8, new BigDecimal("8.00"));

        Map<String, Double> subs = result.getSubScores();
        assertNotNull(subs.get("relevance"));
        assertNotNull(subs.get("price"));
        assertNotNull(subs.get("distance"));
        assertNotNull(subs.get("rating"));
        assertNotNull(subs.get("fulfillment"));
    }

    private static StoreOfferProjection createProjection(Long storeId, Long offerId,
                                                          String status, BigDecimal price,
                                                          BigDecimal rating, int reviews,
                                                          double lat, double lng) {
        StoreOfferProjection p = new StoreOfferProjection();
        p.setStoreId(storeId);
        p.setOfferId(offerId);
        p.setStoreName("Store " + storeId);
        p.setStoreStatus(status);
        p.setStoreLat(lat);
        p.setStoreLng(lng);
        p.setFruitVariety("苹果");
        p.setFruitCategory("水果");
        p.setStandardPricePer500g(price);
        p.setAvgRating(rating);
        p.setReviewCount(reviews);
        return p;
    }
}
