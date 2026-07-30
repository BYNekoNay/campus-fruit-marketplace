-- V3__create_store_staff_table.sql
-- 门店员工表：门店下的员工关联

CREATE TABLE store_staff (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL COMMENT '所属门店 ID',
    user_id BIGINT NOT NULL COMMENT 'identity 用户 ID',
    role VARCHAR(50) NOT NULL DEFAULT 'STAFF' COMMENT '角色：OWNER/MANAGER/STAFF',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_user (store_id, user_id),
    INDEX idx_store_id (store_id),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_staff_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店员工表';
