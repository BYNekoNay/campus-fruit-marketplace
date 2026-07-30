package com.campusfruit.offer.dto;

import java.time.Instant;

public class ReservationResponse {

    private boolean success;
    private String reservationId;
    private Instant expiresAt;

    public ReservationResponse() {}

    public ReservationResponse(boolean success, String reservationId, Instant expiresAt) {
        this.success = success;
        this.reservationId = reservationId;
        this.expiresAt = expiresAt;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
