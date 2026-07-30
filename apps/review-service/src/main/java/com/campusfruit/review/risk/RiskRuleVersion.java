package com.campusfruit.review.risk;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 风控规则版本实体。
 * 记录规则版本号，便于审计规则变更历史。
 */
@Entity
@Table(name = "risk_rule_versions")
public class RiskRuleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion = 1;

    @Column(name = "rule_config_snapshot", columnDefinition = "TEXT")
    private String ruleConfigSnapshot;

    @Column(name = "applied_at", updatable = false)
    private Instant appliedAt;

    @PrePersist
    protected void onCreate() {
        this.appliedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(Integer ruleVersion) { this.ruleVersion = ruleVersion; }

    public String getRuleConfigSnapshot() { return ruleConfigSnapshot; }
    public void setRuleConfigSnapshot(String ruleConfigSnapshot) { this.ruleConfigSnapshot = ruleConfigSnapshot; }

    public Instant getAppliedAt() { return appliedAt; }
}
