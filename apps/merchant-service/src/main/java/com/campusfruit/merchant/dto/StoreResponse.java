package com.campusfruit.merchant.dto;

import com.campusfruit.merchant.enums.StoreStatus;

import java.time.Instant;
import java.util.List;

public class StoreResponse {

    private Long id;
    private Long merchantId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String coordType;
    private String phone;
    private String businessHours;
    private StoreStatus status;
    private String statusText;
    private Integer pickupLeadMinutes;
    private Instant createdAt;
    private Instant updatedAt;
    private List<StaffResponse> staff;

    // --- Getters / Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCoordType() {
        return coordType;
    }

    public void setCoordType(String coordType) {
        this.coordType = coordType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(String businessHours) {
        this.businessHours = businessHours;
    }

    public StoreStatus getStatus() {
        return status;
    }

    public void setStatus(StoreStatus status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public Integer getPickupLeadMinutes() {
        return pickupLeadMinutes;
    }

    public void setPickupLeadMinutes(Integer pickupLeadMinutes) {
        this.pickupLeadMinutes = pickupLeadMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<StaffResponse> getStaff() {
        return staff;
    }

    public void setStaff(List<StaffResponse> staff) {
        this.staff = staff;
    }
}
