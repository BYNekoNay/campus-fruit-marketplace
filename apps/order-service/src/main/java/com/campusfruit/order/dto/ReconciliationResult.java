package com.campusfruit.order.dto;

import java.time.Instant;

/**
 * 对账结果 DTO。
 */
public class ReconciliationResult {

    /** 订单ID */
    private Long orderId;

    /** 执行的操作 */
    private String actionTaken;

    /** 原因说明 */
    private String reason;

    /** 对账时间 */
    private Instant reconciledAt;

    /** 之前的订单状态 */
    private String previousStatus;

    /** 之后的订单状态 */
    private String newStatus;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getReconciledAt() { return reconciledAt; }
    public void setReconciledAt(Instant reconciledAt) { this.reconciledAt = reconciledAt; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
}
