package com.campusfruit.order.dto;

import java.time.Instant;

/**
 * 自取码响应。
 * 仅在 generatePickupCode 时返回明文码，其它场景返回 masked 信息。
 */
public class PickupCodeResponse {

    /** 明文自取码（仅首次生成时返回） */
    private String pickupCode;

    /** 过期时间 */
    private Instant expiresAt;

    /** 剩余有效秒数 */
    private Long expiresIn;

    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
}
