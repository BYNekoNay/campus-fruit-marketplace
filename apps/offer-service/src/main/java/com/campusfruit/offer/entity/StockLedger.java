package com.campusfruit.offer.entity;

import com.campusfruit.offer.enums.StockChangeType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stock_ledger")
public class StockLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    private StockChangeType changeType;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "available_before")
    private Integer availableBefore;

    @Column(name = "available_after")
    private Integer availableAfter;

    @Column(name = "reserved_before")
    private Integer reservedBefore;

    @Column(name = "reserved_after")
    private Integer reservedAfter;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public StockChangeType getChangeType() { return changeType; }
    public void setChangeType(StockChangeType changeType) { this.changeType = changeType; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public Integer getAvailableBefore() { return availableBefore; }
    public void setAvailableBefore(Integer availableBefore) { this.availableBefore = availableBefore; }

    public Integer getAvailableAfter() { return availableAfter; }
    public void setAvailableAfter(Integer availableAfter) { this.availableAfter = availableAfter; }

    public Integer getReservedBefore() { return reservedBefore; }
    public void setReservedBefore(Integer reservedBefore) { this.reservedBefore = reservedBefore; }

    public Integer getReservedAfter() { return reservedAfter; }
    public void setReservedAfter(Integer reservedAfter) { this.reservedAfter = reservedAfter; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
