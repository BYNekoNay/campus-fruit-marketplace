CREATE TABLE review_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL COMMENT '被举报评价ID',
    reporter_id BIGINT NOT NULL COMMENT '举报人ID',
    reason TEXT NULL COMMENT '举报原因',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/DISMISSED/ACCEPTED',
    reviewer_id BIGINT NULL COMMENT '审核人ID',
    review_comment TEXT NULL COMMENT '审核意见',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',

    KEY idx_review_reports_status (status),
    CONSTRAINT fk_review_reports_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价举报表';
