package com.campusfruit.order.dto;

import java.util.List;

/**
 * 切换门店购物车确认信息。
 * 用户切换门店时，返回当前购物车内容及新旧门店信息供用户确认。
 */
public class SwitchCartConfirmDTO {

    /** 当前门店名称 */
    private String currentStoreName;

    /** 新目标门店名称 */
    private String newStoreName;

    /** 当前购物车商品列表 */
    private List<CartItemResponse> items;

    /** 是否需要确认 */
    private boolean requiresConfirmation;

    public String getCurrentStoreName() { return currentStoreName; }
    public void setCurrentStoreName(String currentStoreName) { this.currentStoreName = currentStoreName; }

    public String getNewStoreName() { return newStoreName; }
    public void setNewStoreName(String newStoreName) { this.newStoreName = newStoreName; }

    public List<CartItemResponse> getItems() { return items; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }

    public boolean isRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }
}
