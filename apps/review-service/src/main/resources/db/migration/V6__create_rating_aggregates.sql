CREATE TABLE rating_aggregates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL COMMENT '门店ID',
    avg_rating DECIMAL(3,2) NULL COMMENT '平均评分',
    bayesian_rating DECIMAL(3,2) NULL COMMENT '贝叶斯平均评分',
    total_ratings INT NOT NULL DEFAULT 0 COMMENT '总评价数',
    rating_distribution VARCHAR(200) NULL COMMENT '评分分布(JSON: {"1":2,"2":1,...})',
    version INT NOT NULL DEFAULT 1 COMMENT '聚合版本号',
    calculated_at TIMESTAMP NULL COMMENT '计算时间',

    UNIQUE KEY uk_rating_aggregates_store_id (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店评分聚合表';
