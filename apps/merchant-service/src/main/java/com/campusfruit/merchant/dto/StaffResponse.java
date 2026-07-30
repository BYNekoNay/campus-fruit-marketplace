package com.campusfruit.merchant.dto;

public class StaffResponse {

    private Long userId;
    private String role;
    private String storeName;

    public StaffResponse() {
    }

    public StaffResponse(Long userId, String role, String storeName) {
        this.userId = userId;
        this.role = role;
        this.storeName = storeName;
    }

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

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
}
