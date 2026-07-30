CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '评价用户ID',
    store_id BIGINT NOT NULL COMMENT '被评价门店ID',
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    rating INT NOT NULL COMMENT '评分(1-5)',
    content TEXT NULL COMMENT '评价内容',
    tags VARCHAR(500) NULL COMMENT '标签(逗号分隔)',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/HIDDEN/DELETED',
    current_version INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
    visible BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否可见',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_reviews_order_id (order_id),
    KEY idx_reviews_store_id (store_id),
    KEY idx_reviews_user_id (user_id),
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';
