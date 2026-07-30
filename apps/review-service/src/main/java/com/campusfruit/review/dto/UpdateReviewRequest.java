package com.campusfruit.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class UpdateReviewRequest {

    @Min(1)
    @Max(5)
    private Integer rating;

    private String content;

    private String[] tags;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
}
