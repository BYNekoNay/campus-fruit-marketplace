-- V7: 创建审核日志表
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_type VARCHAR(32) NOT NULL COMMENT '操作类型：REPORT_ACCEPT, REPORT_DISMISS, HIDE_REVIEW, RESTORE_REVIEW',
    review_id BIGINT NOT NULL COMMENT '关联评价ID',
    report_id BIGINT COMMENT '关联举报ID（可选）',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    comment_text TEXT COMMENT '操作备注/审核意见',
    previous_state VARCHAR(100) COMMENT '操作前状态',
    new_state VARCHAR(100) COMMENT '操作后状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_audit_review_id (review_id),
    INDEX idx_audit_operator_id (operator_id),
    INDEX idx_audit_action_type (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核日志';
