package com.campusfruit.merchant.dto;

public class UpdateStoreRequest {

    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String coordType;
    private String phone;
    private String businessHours;
    private Integer pickupLeadMinutes;

    // --- Getters / Setters ---

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

    public Integer getPickupLeadMinutes() {
        return pickupLeadMinutes;
    }

    public void setPickupLeadMinutes(Integer pickupLeadMinutes) {
        this.pickupLeadMinutes = pickupLeadMinutes;
    }
}
