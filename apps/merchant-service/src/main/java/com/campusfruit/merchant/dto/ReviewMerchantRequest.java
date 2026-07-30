package com.campusfruit.merchant.dto;

import jakarta.validation.constraints.NotBlank;

public class ReviewMerchantRequest {

    @NotBlank(message = "操作类型不能为空")
    private String action; // APPROVE / REJECT

    /** 结构化拒绝原因码: LICENSE_INVALID/ADDRESS_UNVERIFIED/PHONE_UNREACHABLE/INFO_INCOMPLETE/OTHER */
    private String reasonCode;

    private String reason;

    // --- Getters / Setters ---

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
