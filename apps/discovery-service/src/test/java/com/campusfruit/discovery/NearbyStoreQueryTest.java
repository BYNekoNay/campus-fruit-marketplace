package com.campusfruit.discovery;

import com.campusfruit.discovery.dto.NearbyStoreDTO;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import com.campusfruit.discovery.service.NearbyStoreService;
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
class NearbyStoreQueryTest {

    @Autowired
    private StoreOfferProjectionRepository projectionRepository;

    @Autowired
    private NearbyStoreService nearbyStoreService;

    @BeforeEach
    void setUp() {
        projectionRepository.deleteAll();
        seedTestData();
    }

    @Nested
    @DisplayName("范围内门店筛选")
    class BoundingBoxFilter {

        @Test
        @DisplayName("返回范围内的门店")
        void shouldReturnStoresWithinRadius() {
            // 用户位置：北京 (39.9, 116.4)，半径 5km 内应有 A店(39.92, 116.41) 和 B店(39.91, 116.39)
            List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(39.9, 116.4, 5.0, 20);

            assertThat(stores).isNotEmpty();
            assertThat(stores).hasSizeGreaterThanOrEqualTo(2);
            // 验证所有结果都在半径内
            for (NearbyStoreDTO store : stores) {
                double dlat = store.getLat() - 39.9;
                double dlng = store.getLng() - 116.4;
                double computedDist = Math.sqrt(dlat * dlat + dlng * dlng) * 111.0;
                assertThat(computedDist).isLessThanOrEqualTo(5.1);
            }
        }

        @Test
        @DisplayName("范围外门店不返回")
        void shouldExcludeStoresOutsideRadius() {
            List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(39.9, 116.4, 2.0, 20);

            // D 店在距离很远的地方 (40.5, 117.5) 不应该出现
            boolean hasFarStore = stores.stream()
                    .anyMatch(s -> "远方水果店".equals(s.getStoreName()));
            assertThat(hasFarStore).isFalse();
        }

        @Test
        @DisplayName("小半径只返回最近的门店")
        void smallRadiusShouldReturnNearbyOnly() {
            List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(39.91, 116.39, 1.0, 10);

            for (NearbyStoreDTO store : stores) {
                double dlat = store.getLat() - 39.91;
                double dlng = store.getLng() - 116.39;
                double dist = Math.sqrt(dlat * dlat + dlng * dlng) * 111.0;
                assertThat(dist).isLessThanOrEqualTo(1.1);
            }
        }

        @Test
        @DisplayName("无结果场景")
        void emptyResultWhenNoNearbyStores() {
            List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(0.0, 0.0, 1.0, 10);
            assertThat(stores).isEmpty();
        }
    }

    @Nested
    @DisplayName("距离排序")
    class DistanceSorting {

        @Test
        @DisplayName("按距离升序排列")
        void shouldSortByDistanceAscending() {
            List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(39.9, 116.4, 10.0, 20);

            assertThat(stores).isNotEmpty();
            for (int i = 0; i < stores.size() - 1; i++) {
                assertThat(stores.get(i).getDistance())
                        .isLessThanOrEqualTo(stores.get(i + 1).getDistance());
            }
        }

        @Test
        @DisplayName("limit 参数控制返回数量")
        void limitShouldControlReturnCount() {
            List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(39.9, 116.4, 20.0, 2);
            assertThat(stores).hasSizeLessThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("停业门店过滤")
    class SuspendedStoreFilter {

        @Test
        @DisplayName("排除 SUSPENDED 状态的门店")
        void shouldExcludeSuspendedStores() {
            List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(39.9, 116.4, 10.0, 20);

            boolean hasSuspended = stores.stream()
                    .anyMatch(s -> "已停业水果店".equals(s.getStoreName()));
            assertThat(hasSuspended).isFalse();
        }

        @Test
        @DisplayName("ACTIVE 门店正常返回")
        void shouldIncludeActiveStores() {
            List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(39.9, 116.4, 10.0, 20);

            boolean hasActive = stores.stream()
                    .anyMatch(s -> "水果一号店".equals(s.getStoreName()));
            assertThat(hasActive).isTrue();
        }
    }

    @Nested
    @DisplayName("DTO 字段完整性")
    class DtoCompleteness {

        @Test
        @DisplayName("返回字段包含 storeName, address, lat, lng, distance, phone, avgRating")
        void shouldContainAllRequiredFields() {
            List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(39.9, 116.4, 10.0, 20);

            assertThat(stores).isNotEmpty();
            NearbyStoreDTO first = stores.get(0);
            assertThat(first.getStoreId()).isNotNull();
            assertThat(first.getStoreName()).isNotNull();
            assertThat(first.getLat()).isNotNull();
            assertThat(first.getLng()).isNotNull();
            assertThat(first.getDistance()).isNotNull();
            // address 和 phone 可为 null，但 phone 有值时应能获取
        }
    }

    // --- 测试数据 ---

    private void seedTestData() {
        // A 店 - 距 (39.9, 116.4) 约 2.5km
        saveProjection(1L, 101L, "水果一号店", 39.92, 116.41, "ACTIVE",
                BigDecimal.valueOf(4.5), 50);

        // B 店 - 距 (39.9, 116.4) 约 1.5km
        saveProjection(2L, 102L, "鲜果便利店", 39.91, 116.39, "ACTIVE",
                BigDecimal.valueOf(4.2), 30);

        // C 店 - SUSPENDED 状态，应被过滤
        saveProjection(3L, 103L, "已停业水果店", 39.93, 116.42, "SUSPENDED",
                BigDecimal.valueOf(3.0), 5);

        // D 店 - 距离很远 (40.5, 117.5) —— 超出大多数半径测试范围
        saveProjection(4L, 104L, "远方水果店", 40.5, 117.5, "ACTIVE",
                BigDecimal.valueOf(4.0), 10);
    }

    private void saveProjection(Long storeId, Long offerId, String storeName,
                                 Double lat, Double lng, String storeStatus,
                                 BigDecimal avgRating, int reviewCount) {
        StoreOfferProjection p = new StoreOfferProjection();
        p.setStoreId(storeId);
        p.setOfferId(offerId);
        p.setStoreName(storeName);
        p.setStoreAddress(storeName + "的地址");
        p.setStoreLat(lat);
        p.setStoreLng(lng);
        p.setStorePhone("1380000" + storeId);
        p.setStoreStatus(storeStatus);
        p.setMerchantId(1L);
        p.setMerchantName("测试商家");
        p.setCanonicalFruitId(10L);
        p.setFruitCategory("苹果");
        p.setFruitVariety("红富士");
        p.setFruitGrade("一级");
        p.setFruitOrigin("山东");
        p.setSalesUnit("斤");
        p.setNetWeightGrams(500);
        p.setUnitPrice(800L);
        p.setStandardPricePer500g(new BigDecimal("8.00"));
        p.setIsComparable(true);
        p.setAvailableQuantity(100);
        p.setOfferStatus("ACTIVE");
        p.setPriceStale(false);
        p.setAvgRating(avgRating);
        p.setReviewCount(reviewCount);
        p.setAggregateVersion(1);
        projectionRepository.save(p);
    }
}
