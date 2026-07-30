package com.campusfruit.review.dto;

import jakarta.validation.constraints.NotBlank;

public class MerchantReplyRequest {

    @NotBlank
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
