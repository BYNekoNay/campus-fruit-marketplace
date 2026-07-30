package com.campusfruit.discovery.analytics;

import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 商家漏斗分析集成测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Merchant funnel analytics tests")
class MerchantFunnelAnalyticsIT {

    @Mock
    private StoreOfferProjectionRepository projectionRepository;

    private FunnelAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new FunnelAnalyticsService(projectionRepository);
    }

    @Test
    @DisplayName("Should track impressions correctly")
    void shouldTrackImpressions() {
        service.trackImpression("user1", Arrays.asList(1L, 2L, 3L));
        service.trackImpression("user1", Collections.singletonList(4L));

        // 通过漏斗查询验证
        StoreOfferProjection p1 = createProjection(1L, 1L);
        StoreOfferProjection p2 = createProjection(2L, 2L);
        when(projectionRepository.findAll()).thenReturn(Arrays.asList(p1, p2));

        MerchantFunnelAnalytics funnel = service.getDailyFunnel(1L, LocalDate.now());

        assertNotNull(funnel);
        assertTrue(funnel.getImpressions() > 0, "应有曝光记录");
    }

    @Test
    @DisplayName("Should track clicks correctly")
    void shouldTrackClicks() {
        service.trackImpression("user2", Arrays.asList(10L, 20L));
        service.trackClick("user2", 10L);

        StoreOfferProjection p = createProjection(1L, 10L);
        when(projectionRepository.findAll()).thenReturn(Collections.singletonList(p));

        MerchantFunnelAnalytics funnel = service.getDailyFunnel(1L, LocalDate.now());

        assertNotNull(funnel);
        assertTrue(funnel.getClicks() >= 1, "应有点击记录");
    }

    @Test
    @DisplayName("Should compute click-through rate")
    void shouldComputeClickThroughRate() {
        MerchantFunnelAnalytics funnel = new MerchantFunnelAnalytics(100L, 15L, 5L, LocalDate.now());

        assertEquals(0.15, funnel.getClickThroughRate(), 0.01);
        assertEquals(1.0 / 3.0, funnel.getConversionRate(), 0.01);
    }

    @Test
    @DisplayName("Should return zero rates for empty funnel")
    void shouldReturnZeroForEmptyFunnel() {
        MerchantFunnelAnalytics funnel = new MerchantFunnelAnalytics(0L, 0L, 0L, LocalDate.now());

        assertEquals(0.0, funnel.getClickThroughRate());
        assertEquals(0.0, funnel.getConversionRate());
    }

    @Test
    @DisplayName("Should give merchant-specific funnel data")
    void shouldGiveMerchantSpecificFunnel() {
        String sessionKey = "test-session-specific";
        service.trackImpression(sessionKey, Arrays.asList(100L, 200L, 300L));
        service.trackClick(sessionKey, 100L);
        service.trackClick(sessionKey, 300L);

        StoreOfferProjection p1 = createProjection(1L, 100L);
        StoreOfferProjection p2 = createProjection(2L, 200L);
        when(projectionRepository.findAll()).thenReturn(Arrays.asList(p1, p2));

        MerchantFunnelAnalytics funnel1 = service.getDailyFunnel(1L, LocalDate.now());
        assertTrue(funnel1.getClicks() >= 1, "商家1应有点击");

        MerchantFunnelAnalytics funnel2 = service.getDailyFunnel(2L, LocalDate.now());
        assertTrue(funnel2.getImpressions() >= 1, "商家2应有曝光");
    }

    private static StoreOfferProjection createProjection(Long merchantId, Long offerId) {
        StoreOfferProjection p = new StoreOfferProjection();
        p.setStoreId(merchantId * 10);
        p.setOfferId(offerId);
        p.setMerchantId(merchantId);
        p.setStoreName("Store-" + merchantId);
        p.setStoreStatus("OPEN");
        return p;
    }
}
