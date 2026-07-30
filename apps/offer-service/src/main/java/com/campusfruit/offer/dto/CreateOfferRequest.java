package com.campusfruit.offer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateOfferRequest {

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @NotNull(message = "标准水果ID不能为空")
    private Long canonicalFruitId;

    @NotBlank(message = "销售单位不能为空")
    @Size(max = 50)
    private String salesUnit;

    private Integer netWeightGrams;

    @NotNull(message = "单位价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private Long unitPrice;

    @NotNull(message = "库存量不能为空")
    @Min(value = 0, message = "库存量不能为负数")
    private Integer stockQuantity;

    private String qualityDesc;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getCanonicalFruitId() { return canonicalFruitId; }
    public void setCanonicalFruitId(Long canonicalFruitId) { this.canonicalFruitId = canonicalFruitId; }

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
