CREATE TABLE price_daily_stats (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  canonical_fruit_id BIGINT NOT NULL,
  stat_date DATE NOT NULL,
  min_price DECIMAL(12,2),
  max_price DECIMAL(12,2),
  median_price DECIMAL(12,2),
  avg_price DECIMAL(12,2),
  store_count INT DEFAULT 0,
  sample_count INT DEFAULT 0,
  UNIQUE KEY uk_fruit_date (canonical_fruit_id, stat_date)
);
