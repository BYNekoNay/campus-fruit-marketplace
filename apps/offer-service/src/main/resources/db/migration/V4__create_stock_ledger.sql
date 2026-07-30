CREATE TABLE stock_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_id BIGINT NOT NULL COMMENT '报价ID',
    change_type VARCHAR(50) NOT NULL COMMENT 'INITIAL/RESERVE/CONFIRM/RELEASE/CANCEL/ADJUST',
    quantity_change INT NOT NULL COMMENT '变动数量',
    available_before INT COMMENT '变动前可用量',
    available_after INT COMMENT '变动后可用量',
    reserved_before INT COMMENT '变动前预占量',
    reserved_after INT COMMENT '变动后预占量',
    reference_id VARCHAR(100) COMMENT '关联订单ID等',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stock_ledger_offer FOREIGN KEY (offer_id) REFERENCES offers(id),

    INDEX idx_stock_ledger_offer_id (offer_id),
    INDEX idx_stock_ledger_change_type (change_type),
    INDEX idx_stock_ledger_reference_id (reference_id),
    INDEX idx_stock_ledger_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存流水';
