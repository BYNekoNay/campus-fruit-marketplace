package com.campusfruit.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SubmitReviewRequest {

    @NotNull
    private Long orderId;

    @NotNull
    private Long storeId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String content;

    private String[] tags;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
}
