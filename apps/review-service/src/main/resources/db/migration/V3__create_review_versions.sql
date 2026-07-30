CREATE TABLE review_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL COMMENT '评价ID',
    version INT NOT NULL COMMENT '版本号',
    rating INT NULL COMMENT '该版本评分',
    content TEXT NULL COMMENT '该版本评价内容',
    tags VARCHAR(500) NULL COMMENT '该版本标签',
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',

    UNIQUE KEY uk_review_versions (review_id, version),
    KEY idx_review_versions_review_id (review_id),
    CONSTRAINT fk_review_versions_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价版本历史表';
