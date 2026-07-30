package com.campusfruit.discovery.analytics;

import java.time.LocalDate;

/**
 * 商家漏斗分析 VO：曝光-点击-下单。
 */
public class MerchantFunnelAnalytics {

    private Long impressions;
    private Long clicks;
    private Long orders;
    private LocalDate date;

    public MerchantFunnelAnalytics() {
    }

    public MerchantFunnelAnalytics(Long impressions, Long clicks, Long orders, LocalDate date) {
        this.impressions = impressions;
        this.clicks = clicks;
        this.orders = orders;
        this.date = date;
    }

    public Long getImpressions() { return impressions; }
    public void setImpressions(Long impressions) { this.impressions = impressions; }

    public Long getClicks() { return clicks; }
    public void setClicks(Long clicks) { this.clicks = clicks; }

    public Long getOrders() { return orders; }
    public void setOrders(Long orders) { this.orders = orders; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    /**
     * 点击率 (CTR)
     */
    public double getClickThroughRate() {
        if (impressions == null || impressions == 0) return 0.0;
        return (double) clicks / impressions;
    }

    /**
     * 转化率 (CVR)
     */
    public double getConversionRate() {
        if (clicks == null || clicks == 0) return 0.0;
        return (double) orders / clicks;
    }
}
