package com.campusfruit.merchant.projection;

import java.util.List;

public class ProjectionExportResponse {

    private List<MerchantProjection> data;
    private String nextToken;
    private boolean hasMore;

    public ProjectionExportResponse() {
    }

    public ProjectionExportResponse(List<MerchantProjection> data, String nextToken, boolean hasMore) {
        this.data = data;
        this.nextToken = nextToken;
        this.hasMore = hasMore;
    }

    public List<MerchantProjection> getData() { return data; }
    public void setData(List<MerchantProjection> data) { this.data = data; }

    public String getNextToken() { return nextToken; }
    public void setNextToken(String nextToken) { this.nextToken = nextToken; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
}
