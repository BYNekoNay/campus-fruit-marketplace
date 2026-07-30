package com.campusfruit.discovery.dto;

import java.util.List;

public class SearchResponse {

    private long totalCount;
    private List<StoreOfferProjectionDTO> items;
    private PriceStatsResponse priceStats;

    public SearchResponse() {
    }

    public SearchResponse(long totalCount, List<StoreOfferProjectionDTO> items) {
        this.totalCount = totalCount;
        this.items = items;
    }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public List<StoreOfferProjectionDTO> getItems() { return items; }
    public void setItems(List<StoreOfferProjectionDTO> items) { this.items = items; }

    public PriceStatsResponse getPriceStats() { return priceStats; }
    public void setPriceStats(PriceStatsResponse priceStats) { this.priceStats = priceStats; }
}
