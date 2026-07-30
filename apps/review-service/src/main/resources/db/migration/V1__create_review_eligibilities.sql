CREATE TABLE review_eligibilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_completed_at TIMESTAMP NULL COMMENT '订单完成时间',
    used BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已使用评价资格',
    tombstone BOOLEAN NOT NULL DEFAULT FALSE COMMENT '订单取消时标记为墓碑',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_review_eligibilities_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价资格表（订单完成时自动创建，订单取消时标记墓碑）';
