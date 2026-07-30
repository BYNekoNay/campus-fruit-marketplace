package com.campusfruit.discovery;

import com.campusfruit.discovery.dto.*;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import com.campusfruit.discovery.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ComparisonQueryIT {

    @Autowired
    private StoreOfferProjectionRepository projectionRepository;

    @Autowired
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        projectionRepository.deleteAll();
        seedTestData();
    }

    @Nested
    @DisplayName("搜索")
    class Search {

        @Test
        @DisplayName("关键词搜索")
        void keywordSearch() {
            SearchRequest request = new SearchRequest();
            request.setKeyword("红富士");

            SearchResponse response = searchService.search(request);
            assertThat(response.getTotalCount()).isGreaterThanOrEqualTo(1);
            assertThat(response.getItems()).isNotEmpty();
            assertThat(response.getItems().get(0).getFruitVariety()).contains("红富士");
        }

        @Test
        @DisplayName("分类搜索")
        void categorySearch() {
            SearchRequest request = new SearchRequest();
            request.setCategory("苹果");

            SearchResponse response = searchService.search(request);
            assertThat(response.getTotalCount()).isGreaterThanOrEqualTo(2);
            response.getItems().forEach(item ->
                    assertThat(item.getFruitVariety()).isNotNull());
        }

        @Test
        @DisplayName("价格排序")
        void priceSort() {
            SearchRequest request = new SearchRequest();
            request.setSortBy(SearchRequest.SortBy.PRICE_ASC);

            SearchResponse response = searchService.search(request);
            List<StoreOfferProjectionDTO> items = response.getItems();
            for (int i = 0; i < items.size() - 1; i++) {
                if (items.get(i).getStandardPricePer500g() != null
                        && items.get(i + 1).getStandardPricePer500g() != null) {
                    assertThat(items.get(i).getStandardPricePer500g())
                            .isLessThanOrEqualTo(items.get(i + 1).getStandardPricePer500g());
                }
            }
        }

        @Test
        @DisplayName("地理范围过滤")
        void geoFilter() {
            SearchRequest request = new SearchRequest();
            request.setLat(39.9);
            request.setLng(116.4);
            request.setRadiusKm(10.0);

            SearchResponse response = searchService.search(request);
            assertThat(response.getTotalCount()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("距离计算")
        void distanceCalculation() {
            SearchRequest request = new SearchRequest();
            request.setLat(39.9);
            request.setLng(116.4);
            request.setRadiusKm(10.0);

            SearchResponse response = searchService.search(request);
            if (!response.getItems().isEmpty()) {
                StoreOfferProjectionDTO first = response.getItems().get(0);
                assertThat(first.getStoreLat()).isNotNull();
                assertThat(first.getStoreLng()).isNotNull();
                // 距离使用 approximate formula
                double expectedDistance = Math.pow(first.getStoreLat() - 39.9, 2)
                        + Math.pow(first.getStoreLng() - 116.4, 2);
                assertThat(first.getDistance()).isEqualTo(expectedDistance);
            }
        }
    }

    @Nested
    @DisplayName("比价")
    class Comparison {

        @Test
        @DisplayName("最多 5 个报价比价")
        void compareMaxFive() {
            CompareResponse response = searchService.getComparison(
                    List.of(1001L, 1002L, 1003L, 1004L, 1005L, 1006L));

            assertThat(response.getOffers()).hasSizeLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("比价返回统计信息")
        void comparisonStats() {
            CompareResponse response = searchService.getComparison(
                    List.of(1001L, 1002L, 1003L));

            assertThat(response.getOffers()).hasSize(3);
            assertThat(response.getStats()).isNotNull();
            assertThat(response.getStats().getMinPrice()).isNotNull();
            assertThat(response.getStats().getMaxPrice()).isNotNull();
            assertThat(response.getStats().getAvgPrice()).isNotNull();

            // 验证 min <= avg <= max
            BigDecimal min = response.getStats().getMinPrice();
            BigDecimal avg = response.getStats().getAvgPrice();
            BigDecimal max = response.getStats().getMaxPrice();

            assertThat(min).isLessThanOrEqualTo(avg);
            assertThat(avg).isLessThanOrEqualTo(max);
        }

        @Test
        @DisplayName("空列表比价")
        void emptyComparison() {
            CompareResponse response = searchService.getComparison(List.of());
            assertThat(response.getOffers()).isEmpty();
            assertThat(response.getStats()).isNotNull();
            assertThat(response.getStats().isSampleInsufficient()).isTrue();
        }

        @Test
        @DisplayName("不存在的报价")
        void nonExistentOffers() {
            CompareResponse response = searchService.getComparison(
                    List.of(9999L, 9998L));
            assertThat(response.getOffers()).isEmpty();
            assertThat(response.getStats().isSampleInsufficient()).isTrue();
        }
    }

    @Nested
    @DisplayName("统计")
    class Statistics {

        @Test
        @DisplayName("价格统计")
        void priceStats() {
            PriceStatsResponse stats = searchService.getPriceStats(10L);

            assertThat(stats).isNotNull();
            assertThat(stats.getMinPrice()).isNotNull();
            assertThat(stats.getSampleCount()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("品类列表")
        void categories() {
            List<String> categories = searchService.getCategories();
            assertThat(categories).isNotEmpty();
            assertThat(categories).contains("苹果");
        }

        @Test
        @DisplayName("门店报价列表")
        void storeOffers() {
            List<StoreOfferProjectionDTO> offers = searchService.getStoreOffers(1L);
            assertThat(offers).isNotEmpty();
            offers.forEach(offer -> assertThat(offer.getStoreId()).isEqualTo(1L));
        }
    }

    @Nested
    @DisplayName("单元价格转元")
    class UnitPriceConversion {

        @Test
        @DisplayName("分转元计算正确")
        void fenToYuan() {
            List<StoreOfferProjectionDTO> offers = searchService.getStoreOffers(1L);
            StoreOfferProjectionDTO apple = offers.stream()
                    .filter(o -> "红富士".equals(o.getFruitVariety()))
                    .findFirst().orElse(null);

            assertThat(apple).isNotNull();
            // 800分 / 100 = 8.00 元
            assertThat(apple.getUnitPriceYuan()).isEqualByComparingTo("8.00");
        }
    }

    // --- 测试数据 ---

    private void seedTestData() {
        saveProjection(1L, 1001L, "水果一号店", 39.9, 116.4, 10L, "苹果", "红富士", "一级",
                800L, 500, 100, "8.00");
        saveProjection(1L, 1002L, "水果一号店", 39.9, 116.4, 10L, "苹果", "红富士", "一级",
                700L, 500, 80, "7.00");
        saveProjection(2L, 1003L, "鲜果便利店", 39.92, 116.41, 10L, "苹果", "红富士", "一级",
                900L, 500, 50, "9.00");
        saveProjection(3L, 1004L, "校园水果铺", 39.88, 116.38, 10L, "苹果", "嘎啦", "一级",
                600L, 500, 120, "6.00");
        saveProjection(3L, 1005L, "校园水果铺", 39.88, 116.38, 20L, "梨", "皇冠梨", "一级",
                500L, 500, 90, "5.00");
    }

    private void saveProjection(Long storeId, Long offerId, String storeName,
                                 Double lat, Double lng, Long fruitId,
                                 String category, String variety, String grade,
                                 Long unitPrice, int netWeight, int quantity,
                                 String standardPrice) {
        StoreOfferProjection p = new StoreOfferProjection();
        p.setStoreId(storeId);
        p.setOfferId(offerId);
        p.setStoreName(storeName);
        p.setStoreLat(lat);
        p.setStoreLng(lng);
        p.setStoreStatus("ACTIVE");
        p.setMerchantId(1L);
        p.setMerchantName("测试商家");
        p.setCanonicalFruitId(fruitId);
        p.setFruitCategory(category);
        p.setFruitVariety(variety);
        p.setFruitGrade(grade);
        p.setSalesUnit("斤");
        p.setNetWeightGrams(netWeight);
        p.setUnitPrice(unitPrice);
        p.setStandardPricePer500g(new BigDecimal(standardPrice));
        p.setIsComparable(true);
        p.setAvailableQuantity(quantity);
        p.setOfferStatus("ACTIVE");
        p.setAvgRating(BigDecimal.valueOf(4.0 + Math.random()));
        p.setReviewCount((int) (10 + Math.random() * 50));
        p.setAggregateVersion(1);
        projectionRepository.save(p);
    }
}
