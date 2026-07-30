package com.campusfruit.discovery.ranking;

import com.campusfruit.discovery.dto.SearchRequest.SortBy;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.ranking.SortModeHandler.RankedOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 排名公式测试：固定数据集验证 5 种排序期望顺序。
 */
@DisplayName("Ranking formula tests")
class RankingFormulaTest {

    private RankingWeights weights;
    private RankingVersionConfig config;
    private RankingFormula formula;
    private SortModeHandler sortModeHandler;

    private StoreOfferProjection cheapNear;
    private StoreOfferProjection expensiveFar;
    private StoreOfferProjection highRated;
    private StoreOfferProjection newStore;
    private StoreOfferProjection closedStore;

    @BeforeEach
    void setUp() {
        weights = new RankingWeights();
        weights.setRelevance(0.30);
        weights.setPrice(0.25);
        weights.setDistance(0.20);
        weights.setRating(0.15);
        weights.setFulfillment(0.10);

        config = new RankingVersionConfig();
        config.setColdStartMaxExplorationSlots(2);
        config.setColdStartMinReviewCount(3);
        config.setColdStartExplorationBonus(0.15);
        config.setColdStartMinRatingThreshold(2.0);
        config.setColdStartLowRatingMinReviewCount(5);
        config.setBayesianPriorMean(3.5);
        config.setBayesianPriorWeight(10);

        formula = new RankingFormula(weights, config);
        sortModeHandler = new SortModeHandler(formula,
                new com.campusfruit.discovery.client.OrderProjectionClient("http://127.0.0.1:1", "test"));

        // 便宜 + 近 = 最佳候选
        cheapNear = createProjection(1L, 1L, "新鲜苹果", "OPEN", 30.9244, 121.4595,
                new BigDecimal("8.00"), new BigDecimal("4.5"), 100);

        // 贵 + 远 = 最差候选
        expensiveFar = createProjection(2L, 2L, "新鲜苹果", "OPEN", 31.2304, 121.4737,
                new BigDecimal("25.00"), new BigDecimal("3.0"), 5);

        // 高评分
        highRated = createProjection(3L, 3L, "新鲜苹果", "OPEN", 30.9350, 121.4600,
                new BigDecimal("15.00"), new BigDecimal("4.9"), 200);

        // 新门店（评论少，冷启动候选）
        newStore = createProjection(4L, 4L, "新鲜苹果", "OPEN", 30.9300, 121.4580,
                new BigDecimal("10.00"), new BigDecimal("4.0"), 0);

        // 已关闭门店（不应进入排名）
        closedStore = createProjection(5L, 5L, "新鲜苹果", "CLOSED", 30.9250, 121.4600,
                new BigDecimal("5.00"), new BigDecimal("4.8"), 50);
    }

    @Test
    @DisplayName("COMPREHENSIVE: close cheap store should rank higher than far expensive")
    void comprehensiveSortShouldPreferCheapNear() {
        List<StoreOfferProjection> projections = Arrays.asList(expensiveFar, cheapNear);
        List<RankedOffer> sorted = sortModeHandler.sort(projections, SortBy.COMPREHENSIVE,
                30.9280, 121.4560, "苹果");

        assertEquals(2, sorted.size());
        assertTrue(sorted.get(0).result.getScore() > sorted.get(1).result.getScore(),
                "便宜近的应排在前面");
    }

    @Test
    @DisplayName("DISTANCE: closest store should rank first")
    void distanceSortShouldPreferClosest() {
        List<StoreOfferProjection> projections = Arrays.asList(expensiveFar, cheapNear, highRated);
        List<RankedOffer> sorted = sortModeHandler.sort(projections, SortBy.DISTANCE,
                30.9280, 121.4560, null);

        assertEquals(3, sorted.size());
        double d1 = sorted.get(0).result.getSubScores().getOrDefault("distanceKm", Double.MAX_VALUE);
        double d2 = sorted.get(1).result.getSubScores().getOrDefault("distanceKm", Double.MAX_VALUE);
        double d3 = sorted.get(2).result.getSubScores().getOrDefault("distanceKm", Double.MAX_VALUE);
        assertTrue(d1 <= d2);
        assertTrue(d2 <= d3);
    }

    @Test
    @DisplayName("PRICE_ASC: cheapest should rank first")
    void priceAscSortShouldPreferCheapest() {
        List<StoreOfferProjection> projections = Arrays.asList(expensiveFar, cheapNear, highRated);
        List<RankedOffer> sorted = sortModeHandler.sort(projections, SortBy.PRICE_ASC,
                null, null, null);

        assertEquals(3, sorted.size());
        assertEquals(new BigDecimal("8.00"), sorted.get(0).projection.getStandardPricePer500g());
        assertEquals(new BigDecimal("15.00"), sorted.get(1).projection.getStandardPricePer500g());
        assertEquals(new BigDecimal("25.00"), sorted.get(2).projection.getStandardPricePer500g());
    }

    @Test
    @DisplayName("PRICE_DESC: most expensive should rank first")
    void priceDescSortShouldPreferExpensive() {
        List<StoreOfferProjection> projections = Arrays.asList(cheapNear, expensiveFar, highRated);
        List<RankedOffer> sorted = sortModeHandler.sort(projections, SortBy.PRICE_DESC,
                null, null, null);

        assertEquals(3, sorted.size());
        assertEquals(new BigDecimal("25.00"), sorted.get(0).projection.getStandardPricePer500g());
        assertEquals(new BigDecimal("8.00"), sorted.get(2).projection.getStandardPricePer500g());
    }

    @Test
    @DisplayName("RATING: highest rated should rank first")
    void ratingSortShouldPreferHighestRated() {
        List<StoreOfferProjection> projections = Arrays.asList(cheapNear, expensiveFar, highRated);
        List<RankedOffer> sorted = sortModeHandler.sort(projections, SortBy.RATING,
                null, null, null);

        assertEquals(3, sorted.size());
        assertEquals(new BigDecimal("4.9"), sorted.get(0).projection.getAvgRating());
    }

    @Test
    @DisplayName("SALES: most reviews should rank first")
    void salesSortShouldPreferMostReviewed() {
        List<StoreOfferProjection> projections = Arrays.asList(cheapNear, expensiveFar, highRated, newStore);
        List<RankedOffer> sorted = sortModeHandler.sort(projections, SortBy.SALES,
                null, null, null);

        assertEquals(4, sorted.size());
        assertEquals(200, sorted.get(0).projection.getReviewCount());
    }

    @Test
    @DisplayName("Cold start: closed stores should be filtered out")
    void coldStartShouldFilterClosedStores() {
        ColdStartHandler handler = new ColdStartHandler(config);
        assertFalse(handler.isEligible(closedStore), "CLOSED 门店不应有资格");
        assertTrue(handler.isEligible(cheapNear), "OPEN 门店应有资格");
    }

    @Test
    @DisplayName("Cold start: new store identified correctly")
    void coldStartShouldIdentifyNewStore() {
        ColdStartHandler handler = new ColdStartHandler(config);
        assertTrue(handler.isNewStore(newStore), "评论数 0 应被识别为新门店");
        assertFalse(handler.isNewStore(cheapNear), "评论数 100 不应该是新门店");
    }

    private static StoreOfferProjection createProjection(Long storeId, Long offerId,
                                                          String variety, String status,
                                                          double lat, double lng,
                                                          BigDecimal price, BigDecimal rating,
                                                          int reviews) {
        StoreOfferProjection p = new StoreOfferProjection();
        p.setStoreId(storeId);
        p.setOfferId(offerId);
        p.setStoreName("Store " + storeId);
        p.setStoreStatus(status);
        p.setStoreLat(lat);
        p.setStoreLng(lng);
        p.setFruitVariety(variety);
        p.setFruitCategory("水果");
        p.setStandardPricePer500g(price);
        p.setAvgRating(rating);
        p.setReviewCount(reviews);
        p.setLastEventAt(Instant.now().minus(60, ChronoUnit.DAYS));
        return p;
    }
}
