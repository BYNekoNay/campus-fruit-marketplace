package com.campusfruit.order.dto;

import jakarta.validation.constraints.NotNull;

public class SwitchCartConfirmRequest {

    @NotNull(message = "新门店ID不能为空")
    private Long newStoreId;

    private String newStoreName;

    public Long getNewStoreId() { return newStoreId; }
    public void setNewStoreId(Long newStoreId) { this.newStoreId = newStoreId; }

    public String getNewStoreName() { return newStoreName; }
    public void setNewStoreName(String newStoreName) { this.newStoreName = newStoreName; }
}
