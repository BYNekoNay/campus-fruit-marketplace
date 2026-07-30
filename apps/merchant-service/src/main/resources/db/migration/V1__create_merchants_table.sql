-- V1__create_merchants_table.sql
-- 商家表：存储商家基本信息，入驻需审核

CREATE TABLE merchants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL COMMENT '关联 identity 用户 ID',
    name VARCHAR(200) NOT NULL COMMENT '商家名称',
    contact_name VARCHAR(100) COMMENT '联系人姓名',
    contact_phone VARCHAR(20) COMMENT '联系人电话',
    license_number VARCHAR(100) COMMENT '营业执照号',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '状态：PENDING_REVIEW/APPROVED/REJECTED/SUSPENDED',
    reject_reason TEXT COMMENT '拒绝原因',
    reviewed_by BIGINT COMMENT '审核人 ID',
    reviewed_at TIMESTAMP COMMENT '审核时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner_user_id (owner_user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家表';
