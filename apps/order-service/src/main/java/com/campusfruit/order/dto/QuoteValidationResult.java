package com.campusfruit.order.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 报价校验结果：下单前比对购物车快照价格与当前报价。
 */
public class QuoteValidationResult {

    private boolean valid = true;
    private boolean priceChanged;
    private boolean stockChanged;
    private boolean storeStatusChanged;
    private Long currentUnitPrice;
    private Long snapshotUnitPrice;
    private Integer currentOfferVersion;
    private Integer snapshotOfferVersion;
    private List<ChangeDetail> changes = new ArrayList<>();

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public boolean isPriceChanged() { return priceChanged; }
    public void setPriceChanged(boolean priceChanged) { this.priceChanged = priceChanged; }

    public boolean isStockChanged() { return stockChanged; }
    public void setStockChanged(boolean stockChanged) { this.stockChanged = stockChanged; }

    public boolean isStoreStatusChanged() { return storeStatusChanged; }
    public void setStoreStatusChanged(boolean storeStatusChanged) { this.storeStatusChanged = storeStatusChanged; }

    public Long getCurrentUnitPrice() { return currentUnitPrice; }
    public void setCurrentUnitPrice(Long currentUnitPrice) { this.currentUnitPrice = currentUnitPrice; }

    public Long getSnapshotUnitPrice() { return snapshotUnitPrice; }
    public void setSnapshotUnitPrice(Long snapshotUnitPrice) { this.snapshotUnitPrice = snapshotUnitPrice; }

    public Integer getCurrentOfferVersion() { return currentOfferVersion; }
    public void setCurrentOfferVersion(Integer currentOfferVersion) { this.currentOfferVersion = currentOfferVersion; }

    public Integer getSnapshotOfferVersion() { return snapshotOfferVersion; }
    public void setSnapshotOfferVersion(Integer snapshotOfferVersion) { this.snapshotOfferVersion = snapshotOfferVersion; }

    public List<ChangeDetail> getChanges() { return changes; }
    public void setChanges(List<ChangeDetail> changes) { this.changes = changes; }
    public void addChange(ChangeDetail detail) {
        this.changes.add(detail);
        this.valid = false;
    }

    public static class ChangeDetail {
        private String field;
        private String description;

        public ChangeDetail() {}
        public ChangeDetail(String field, String description) {
            this.field = field;
            this.description = description;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
