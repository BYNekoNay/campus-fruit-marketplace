package com.campusfruit.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 用户授权记录表。
 * <p>
 * 独立于账号 consent，用于管理 LOCATION 等授权类型。
 * 撤销 consent 不删除本记录，仅更新状态（保留审计轨迹）。
 */
@Entity
@Table(name = "user_consents", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_consent_type", columnNames = {"user_id", "consent_type"})
})
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "consent_type", nullable = false, length = 50)
    private String consentType;

    @Column(length = 20, nullable = false)
    private String status = "NOT_SET";

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    // 软删除：true 表示用户已请求删除该授权记录
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.lastUpdated = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getConsentType() { return consentType; }
    public void setConsentType(String consentType) { this.consentType = consentType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
