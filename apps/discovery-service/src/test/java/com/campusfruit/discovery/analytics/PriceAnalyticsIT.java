package com.campusfruit.discovery.analytics;

import com.campusfruit.discovery.entity.PriceDailyStat;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.ranking.PriceStatsAggregationJob;
import com.campusfruit.discovery.repository.PriceDailyStatRepository;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 价格统计聚合测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Price analytics integration tests")
class PriceAnalyticsIT {

    @Mock
    private StoreOfferProjectionRepository projectionRepository;

    @Mock
    private PriceDailyStatRepository statsRepository;

    @InjectMocks
    private PriceStatsAggregationJob job;

    @Test
    @DisplayName("Should aggregate price stats by canonical fruit ID")
    void shouldAggregatePriceStatsByFruitId() {
        // 准备测试数据：同一款苹果在 3 家门店
        List<StoreOfferProjection> projections = Arrays.asList(
                createProjection(1L, 101L, "苹果", "8.00"),
                createProjection(2L, 101L, "苹果", "12.00"),
                createProjection(3L, 101L, "苹果", "10.00"),
                createProjection(4L, 102L, "香蕉", "5.00"),
                createProjection(5L, 102L, "香蕉", "7.00")
        );

        when(projectionRepository.findAll()).thenReturn(projections);

        job.aggregateDailyStats();

        // 应保存 2 条记录（苹果 + 香蕉）
        ArgumentCaptor<PriceDailyStat> captor = ArgumentCaptor.forClass(PriceDailyStat.class);
        verify(statsRepository, times(2)).save(captor.capture());

        List<PriceDailyStat> saved = captor.getAllValues();

        PriceDailyStat apple = saved.stream()
                .filter(s -> s.getCanonicalFruitId() == 101L)
                .findFirst().orElse(null);
        assertNotNull(apple);
        assertEquals(0, new BigDecimal("8.00").compareTo(apple.getMinPrice()), "苹果最低价应为 8.00");
        assertEquals(0, new BigDecimal("12.00").compareTo(apple.getMaxPrice()), "苹果最高价应为 12.00");
        assertEquals(0, new BigDecimal("10.00").compareTo(apple.getMedianPrice()), "苹果中位数应为 10.00");
        assertEquals(3, apple.getSampleCount());
        assertEquals(3, apple.getStoreCount());
        assertEquals(LocalDate.now().minusDays(1), apple.getStatDate());

        PriceDailyStat banana = saved.stream()
                .filter(s -> s.getCanonicalFruitId() == 102L)
                .findFirst().orElse(null);
        assertNotNull(banana);
        assertEquals(0, new BigDecimal("5.00").compareTo(banana.getMinPrice()));
        assertEquals(0, new BigDecimal("7.00").compareTo(banana.getMaxPrice()));
        assertEquals(2, banana.getSampleCount());
    }

    @Test
    @DisplayName("Should skip null canonical fruit IDs")
    void shouldSkipNullFruitIds() {
        List<StoreOfferProjection> projections = Arrays.asList(
                createProjection(1L, null, null, "10.00"),
                createProjection(2L, 101L, "苹果", "15.00")
        );

        when(projectionRepository.findAll()).thenReturn(projections);

        job.aggregateDailyStats();

        verify(statsRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should handle empty projections gracefully")
    void shouldHandleEmptyProjections() {
        when(projectionRepository.findAll()).thenReturn(List.of());

        job.aggregateDailyStats();

        verify(statsRepository, never()).save(any());
    }

    private static StoreOfferProjection createProjection(Long storeId, Long canonicalFruitId,
                                                          String variety, String priceStr) {
        StoreOfferProjection p = new StoreOfferProjection();
        p.setStoreId(storeId);
        p.setOfferId(storeId * 10);
        p.setCanonicalFruitId(canonicalFruitId);
        p.setFruitVariety(variety);
        p.setStandardPricePer500g(new BigDecimal(priceStr));
        p.setStoreStatus("OPEN");
        return p;
    }
}
