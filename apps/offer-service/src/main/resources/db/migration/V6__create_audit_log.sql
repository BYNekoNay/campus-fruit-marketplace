-- V6__create_audit_log.sql
-- 审计日志表：记录所有关键操作

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT COMMENT '操作人 ID',
    actor_type VARCHAR(50) COMMENT '操作人类型',
    action VARCHAR(100) NOT NULL COMMENT '操作动作',
    target_type VARCHAR(100) COMMENT '目标类型',
    target_id VARCHAR(255) COMMENT '目标 ID',
    old_value TEXT COMMENT '旧值',
    new_value TEXT COMMENT '新值',
    reason TEXT COMMENT '操作原因',
    ip_address VARCHAR(45) COMMENT '操作 IP 地址',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_actor_id (actor_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
