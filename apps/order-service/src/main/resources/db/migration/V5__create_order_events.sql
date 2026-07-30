CREATE TABLE order_status_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    from_status VARCHAR(30) COMMENT '原状态',
    to_status VARCHAR(30) NOT NULL COMMENT '目标状态',
    operator_type VARCHAR(50) COMMENT '操作者类型（SYSTEM/USER/STORE_STAFF/ADMIN）',
    operator_id BIGINT COMMENT '操作者ID',
    note TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_status_events_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,

    INDEX idx_order_status_events_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态变更历史';
