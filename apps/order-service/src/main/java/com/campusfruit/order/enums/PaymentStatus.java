package com.campusfruit.order.enums;

/**
 * 支付状态枚举。
 * <p>
 * 与订单状态解耦，当前仅支持取货时支付（PAID_AT_PICKUP）。
 */
public enum PaymentStatus {

    /** 未支付 */
    UNPAID("未支付"),

    /** 取货时已支付 */
    PAID_AT_PICKUP("取货时已支付");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
