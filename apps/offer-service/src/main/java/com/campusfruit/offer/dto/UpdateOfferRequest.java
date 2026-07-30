package com.campusfruit.offer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UpdateOfferRequest {

    @Size(max = 50)
    private String salesUnit;

    private Integer netWeightGrams;

    @Min(value = 0, message = "价格不能为负数")
    private Long unitPrice;

    @Min(value = 0, message = "库存量不能为负数")
    private Integer stockQuantity;

    private String qualityDesc;

    public String getSalesUnit() { return salesUnit; }
    public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }

    public Integer getNetWeightGrams() { return netWeightGrams; }
    public void setNetWeightGrams(Integer netWeightGrams) { this.netWeightGrams = netWeightGrams; }

    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getQualityDesc() { return qualityDesc; }
    public void setQualityDesc(String qualityDesc) { this.qualityDesc = qualityDesc; }
}
