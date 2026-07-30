package com.campusfruit.discovery.dto;

import java.math.BigDecimal;

public class NearbyStoreDTO {

    private Long storeId;
    private String storeName;
    private String address;
    private Double lat;
    private Double lng;
    private Double distance;
    private String phone;
    private BigDecimal avgRating;

    public NearbyStoreDTO() {
    }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) {
        this.distance = distance != null ? Math.round(distance * 100.0) / 100.0 : null;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }
}
