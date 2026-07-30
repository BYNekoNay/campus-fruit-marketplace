package com.campusfruit.merchant.projection;

import java.util.List;

public class MerchantProjection {

    private Long merchantId;
    private String name;
    private String status;
    private List<StoreProjection> stores;

    public MerchantProjection() {
    }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<StoreProjection> getStores() { return stores; }
    public void setStores(List<StoreProjection> stores) { this.stores = stores; }
}
