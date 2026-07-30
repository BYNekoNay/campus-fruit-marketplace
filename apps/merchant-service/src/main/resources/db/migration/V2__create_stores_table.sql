-- V2__create_stores_table.sql
-- 门店表：商家下的门店/自提点

CREATE TABLE stores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL COMMENT '所属商家 ID',
    name VARCHAR(200) NOT NULL COMMENT '门店名称',
    address TEXT NOT NULL COMMENT '门店地址',
    latitude DOUBLE NOT NULL COMMENT '纬度',
    longitude DOUBLE NOT NULL COMMENT '经度',
    coord_type VARCHAR(20) NOT NULL DEFAULT 'BD09LL' COMMENT '坐标类型：BD09LL/GCJ02/WGS84',
    phone VARCHAR(20) COMMENT '门店电话',
    business_hours VARCHAR(500) COMMENT '营业时间（JSON 格式）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL' COMMENT '状态：PENDING_APPROVAL/ACTIVE/CLOSED/SUSPENDED',
    pickup_lead_minutes INT NOT NULL DEFAULT 15 COMMENT '备货时长（分钟）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_status (status),
    INDEX idx_location (latitude, longitude),
    CONSTRAINT fk_stores_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店表';
