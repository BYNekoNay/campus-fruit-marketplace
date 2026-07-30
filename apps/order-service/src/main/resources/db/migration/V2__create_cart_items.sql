CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL COMMENT '购物车ID',
    offer_id BIGINT NOT NULL COMMENT '报价ID',
    canonical_fruit_id BIGINT COMMENT '标准水果ID',
    fruit_variety VARCHAR(200) COMMENT '水果品种',
    sales_unit VARCHAR(50) COMMENT '销售单位',
    unit_price BIGINT NOT NULL COMMENT '单价（分）',
    quantity INT DEFAULT 1 COMMENT '数量',
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',

    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,

    INDEX idx_cart_items_cart_id (cart_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车商品';
