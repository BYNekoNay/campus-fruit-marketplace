package com.campusfruit.order.enums;

/**
 * 订单状态枚举。
 * <p>
 * 状态转移通过 OrderStateMachine 校验，确保转移合法性。
 */
public enum OrderStatus {

    /** 待预占 */
    PENDING_RESERVATION("待预占"),

    /** 待门店确认 */
    PENDING_STORE_CONFIRMATION("待门店确认"),

    /** 已接单 */
    ACCEPTED("已接单"),

    /** 备货完成，待自取 */
    READY_FOR_PICKUP("待自取"),

    /** 已完成（核销） */
    COMPLETED("已完成"),

    /** 已取消 */
    CANCELLED("已取消"),

    /** 已拒绝（预占失败/门店拒单） */
    REJECTED("已拒绝"),

    /** 已过期 */
    EXPIRED("���过期"),

    /** 未取货待处理 */
    NO_SHOW_PENDING("未取货待处理");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
