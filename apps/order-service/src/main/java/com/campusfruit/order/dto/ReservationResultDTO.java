package com.campusfruit.order.dto;

import java.time.Instant;

/**
 * 库存预占结果 DTO。
 */
public class ReservationResultDTO {

    /** 预占是否成功 */
    private boolean success;

    /** 预占ID（成功时有效） */
    private String reservationId;

    /** 预占过期时间 */
    private Instant expiresAt;

    /** 预占数量 */
    private Integer reservedQuantity;

    /** 失败原因（预占失败时有效） */
    private String reason;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static ReservationResultDTO success(String reservationId, Instant expiresAt, Integer reservedQuantity) {
        ReservationResultDTO dto = new ReservationResultDTO();
        dto.setSuccess(true);
        dto.setReservationId(reservationId);
        dto.setExpiresAt(expiresAt);
        dto.setReservedQuantity(reservedQuantity);
        return dto;
    }

    public static ReservationResultDTO failure(String reason) {
        ReservationResultDTO dto = new ReservationResultDTO();
        dto.setSuccess(false);
        dto.setReason(reason);
        return dto;
    }
}
