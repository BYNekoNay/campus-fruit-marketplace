CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    status VARCHAR(30) NOT NULL COMMENT '订单状态',
    total_amount BIGINT NOT NULL DEFAULT 0 COMMENT '总金额（分）',
    item_count INT DEFAULT 0 COMMENT '商品数量',
    idempotency_key VARCHAR(100) COMMENT '幂等键',
    pickup_code_hash VARCHAR(255) COMMENT '自取码哈希',
    pickup_code_expires_at TIMESTAMP COMMENT '自取码过期时间',
    quote_version INT COMMENT '报价版本',
    reservation_id VARCHAR(100) COMMENT '预占ID',
    payment_status VARCHAR(20) DEFAULT 'UNPAID' COMMENT '支付状态',
    cancel_reason TEXT COMMENT '取消原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_orders_order_no (order_no),
    UNIQUE KEY uk_orders_idempotency_key (idempotency_key),

    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_store_id (store_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_user_id_status (user_id, status),
    INDEX idx_orders_store_id_status (store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
