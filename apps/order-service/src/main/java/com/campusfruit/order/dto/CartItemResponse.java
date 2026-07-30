package com.campusfruit.order.dto;

public class CartItemResponse {

    private Long id;
    private Long offerId;
    private String fruitVariety;
    private String salesUnit;
    private Long unitPrice;
    private Integer quantity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public String getFruitVariety() { return fruitVariety; }
    public void setFruitVariety(String fruitVariety) { this.fruitVariety = fruitVariety; }

    public String getSalesUnit() { return salesUnit; }
    public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }

    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
