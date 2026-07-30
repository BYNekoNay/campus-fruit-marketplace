CREATE TABLE offers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL COMMENT '门店ID',
    canonical_fruit_id BIGINT NOT NULL COMMENT '标准水果ID',
    sales_unit VARCHAR(50) NOT NULL COMMENT '销售单位如500g盒装/个/kg',
    net_weight_grams INT COMMENT '净重克数，不可比报价为NULL',
    unit_price BIGINT NOT NULL COMMENT '单位价格，单位：分',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '库存总量',
    available_quantity INT NOT NULL DEFAULT 0 COMMENT '可用量',
    reserved_quantity INT NOT NULL DEFAULT 0 COMMENT '预占量',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/EXPIRED',
    quality_desc TEXT COMMENT '质量说明',
    last_confirmed_at TIMESTAMP COMMENT '商家确认时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_offers_canonical_fruit FOREIGN KEY (canonical_fruit_id) REFERENCES canonical_fruits(id),

    INDEX idx_offers_store_id (store_id),
    INDEX idx_offers_canonical_fruit_id (canonical_fruit_id),
    INDEX idx_offers_store_canonical (store_id, canonical_fruit_id),
    INDEX idx_offers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店报价';
