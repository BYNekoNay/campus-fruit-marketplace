package com.campusfruit.merchant.dto;

import jakarta.validation.constraints.NotNull;

public class AddStaffRequest {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    private String role;

    // --- Getters / Setters ---

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
