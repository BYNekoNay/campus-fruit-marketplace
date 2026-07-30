package com.campusfruit.offer.domain.inventory;

import java.time.LocalDateTime;

/**
 * 库存预占结果 VO。
 */
public class ReservationResult {

    private boolean success;
    private String reservationId;
    private Long offerId;
    private int reservedQuantity;
    private LocalDateTime expiresAt;
    private String rejectReason;

    // 静态工厂方法
    public static ReservationResult success(String reservationId, Long offerId, int quantity, LocalDateTime expiresAt) {
        ReservationResult r = new ReservationResult();
        r.success = true;
        r.reservationId = reservationId;
        r.offerId = offerId;
        r.reservedQuantity = quantity;
        r.expiresAt = expiresAt;
        return r;
    }

    public static ReservationResult failure(String reservationId, String rejectReason) {
        ReservationResult r = new ReservationResult();
        r.success = false;
        r.reservationId = reservationId;
        r.rejectReason = rejectReason;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
