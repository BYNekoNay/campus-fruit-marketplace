package com.campusfruit.merchant.integration.baidu.model;

public class BaiduGeoResult {
    private int status;
    private double lat;
    private double lng;
    private int precise;
    private int confidence;
    private String level;
    private String formattedAddress;

    public BaiduGeoResult() {
    }

    public BaiduGeoResult(int status, double lat, double lng, int precise, int confidence,
                          String level, String formattedAddress) {
        this.status = status;
        this.lat = lat;
        this.lng = lng;
        this.precise = precise;
        this.confidence = confidence;
        this.level = level;
        this.formattedAddress = formattedAddress;
    }

    public boolean isSuccess() {
        return status == 0;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public int getPrecise() { return precise; }
    public void setPrecise(int precise) { this.precise = precise; }

    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getFormattedAddress() { return formattedAddress; }
    public void setFormattedAddress(String formattedAddress) { this.formattedAddress = formattedAddress; }
}
