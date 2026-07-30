CREATE TABLE idempotency_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL COMMENT '幂等键',
    subject_id BIGINT COMMENT '主体ID（用户ID）',
    endpoint VARCHAR(200) COMMENT '端点路径',
    request_body_hash VARCHAR(64) COMMENT '请求体哈希',
    response_body TEXT COMMENT '响应体',
    status VARCHAR(20) NOT NULL COMMENT '状态（PROCESSING/COMPLETED/REJECTED）',
    resource_id VARCHAR(100) COMMENT '资源ID（如order_id）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP COMMENT '过期时间',

    UNIQUE KEY uk_idempotency_records_key (idempotency_key),

    INDEX idx_idempotency_records_subject (subject_id),
    INDEX idx_idempotency_records_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='幂等记录表';
