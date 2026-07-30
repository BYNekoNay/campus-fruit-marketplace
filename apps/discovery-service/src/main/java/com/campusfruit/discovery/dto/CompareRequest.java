package com.campusfruit.discovery.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public class CompareRequest {

    @Size(min = 1, max = 5, message = "比价最多支持 5 个报价")
    private List<Long> offerIds;

    public List<Long> getOfferIds() { return offerIds; }
    public void setOfferIds(List<Long> offerIds) { this.offerIds = offerIds; }
}
