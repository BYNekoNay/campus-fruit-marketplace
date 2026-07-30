CREATE TABLE outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL COMMENT '聚合类型',
    aggregate_id VARCHAR(100) NOT NULL COMMENT '聚合ID',
    event_type VARCHAR(200) NOT NULL COMMENT '事件类型',
    payload TEXT COMMENT '事件载荷（JSON）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published BOOLEAN DEFAULT FALSE COMMENT '是否已发布',

    INDEX idx_outbox_events_published (published),
    INDEX idx_outbox_events_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发件箱事件表';
