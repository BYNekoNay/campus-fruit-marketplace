package com.campusfruit.offer.projection;

import java.util.List;

public class ProjectionExportResponse {

    private List<OfferProjection> data;
    private String nextToken;
    private boolean hasMore;

    public ProjectionExportResponse() {}

    public ProjectionExportResponse(List<OfferProjection> data, String nextToken, boolean hasMore) {
        this.data = data;
        this.nextToken = nextToken;
        this.hasMore = hasMore;
    }

    public List<OfferProjection> getData() { return data; }
    public void setData(List<OfferProjection> data) { this.data = data; }

    public String getNextToken() { return nextToken; }
    public void setNextToken(String nextToken) { this.nextToken = nextToken; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
}
