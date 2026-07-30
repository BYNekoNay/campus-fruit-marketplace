package com.campusfruit.order.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "canonical_fruit_id")
    private Long canonicalFruitId;

    @Column(name = "fruit_variety", length = 200)
    private String fruitVariety;

    @Column(name = "sales_unit", length = 50)
    private String salesUnit;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "offer_version")
    private Integer offerVersion;

    @Column(name = "added_at")
    private Instant addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public Long getCanonicalFruitId() { return canonicalFruitId; }
    public void setCanonicalFruitId(Long canonicalFruitId) { this.canonicalFruitId = canonicalFruitId; }

    public String getFruitVariety() { return fruitVariety; }
    public void setFruitVariety(String fruitVariety) { this.fruitVariety = fruitVariety; }

    public String getSalesUnit() { return salesUnit; }
    public void setSalesUnit(String salesUnit) { this.salesUnit = salesUnit; }

    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getOfferVersion() { return offerVersion; }
    public void setOfferVersion(Integer offerVersion) { this.offerVersion = offerVersion; }

    public Instant getAddedAt() { return addedAt; }
}
