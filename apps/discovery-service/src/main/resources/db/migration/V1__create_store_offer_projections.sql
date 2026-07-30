CREATE TABLE store_offer_projections (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  store_id BIGINT NOT NULL,
  offer_id BIGINT NOT NULL,

  -- 门店信息（来自 Merchant 事件）
  store_name VARCHAR(200) NOT NULL,
  store_address TEXT,
  store_lat DOUBLE,
  store_lng DOUBLE,
  store_phone VARCHAR(20),
  store_status VARCHAR(20),
  merchant_id BIGINT,
  merchant_name VARCHAR(200),

  -- 水果报价信息（来自 Offer 事件）
  canonical_fruit_id BIGINT,
  fruit_category VARCHAR(100),
  fruit_variety VARCHAR(200),
  fruit_grade VARCHAR(50),
  fruit_origin VARCHAR(200),
  sales_unit VARCHAR(50),
  net_weight_grams INT,
  unit_price BIGINT,
  standard_price_per500g DECIMAL(12,2),
  is_comparable BOOLEAN DEFAULT TRUE,
  available_quantity INT DEFAULT 0,
  offer_status VARCHAR(20),
  price_stale BOOLEAN DEFAULT FALSE,

  -- 评分信息（来自 Review 事件，先预留）
  avg_rating DECIMAL(3,2) DEFAULT 0,
  review_count INT DEFAULT 0,

  -- 事件溯源
  aggregate_version INT DEFAULT 1,
  last_event_type VARCHAR(100),
  last_event_at TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_offer (offer_id),
  INDEX idx_store (store_id),
  INDEX idx_fruit (canonical_fruit_id),
  INDEX idx_location (store_lat, store_lng),
  INDEX idx_status (store_status, offer_status)
);
