CREATE TABLE projection_checkpoints (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_service VARCHAR(50) NOT NULL,
  last_event_id VARCHAR(100),
  last_source_sequence BIGINT DEFAULT 0,
  updated_at TIMESTAMP,
  UNIQUE KEY uk_source (source_service)
);
