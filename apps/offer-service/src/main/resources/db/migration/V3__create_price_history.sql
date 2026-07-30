CREATE TABLE price_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_id BIGINT NOT NULL COMMENT '报价ID',
    unit_price BIGINT NOT NULL COMMENT '单位价格，单位：分',
    net_weight_grams INT COMMENT '净重克数',
    sales_unit VARCHAR(50) COMMENT '销售单位',
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_price_histories_offer FOREIGN KEY (offer_id) REFERENCES offers(id),

    INDEX idx_price_histories_offer_id (offer_id),
    INDEX idx_price_histories_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格历史';
