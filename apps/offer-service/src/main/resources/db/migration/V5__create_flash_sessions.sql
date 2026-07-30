CREATE TABLE flash_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_id BIGINT NOT NULL COMMENT '报价ID',
    flash_price BIGINT COMMENT '秒杀价，单位：分',
    flash_quantity INT COMMENT '秒杀数量',
    start_time TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP COMMENT '结束时间',
    status VARCHAR(20) DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED/ACTIVE/ENDED/CANCELLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_flash_sessions_offer FOREIGN KEY (offer_id) REFERENCES offers(id),

    INDEX idx_flash_sessions_offer_id (offer_id),
    INDEX idx_flash_sessions_status (status),
    INDEX idx_flash_sessions_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀场次（预留给U10）';
