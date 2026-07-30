-- V8: 创建风控规则版本表
CREATE TABLE IF NOT EXISTS risk_rule_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_version INT NOT NULL DEFAULT 1 COMMENT '规则版本号',
    rule_config_snapshot TEXT COMMENT '规则配置快照(JSON)',
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
    INDEX idx_rule_version (rule_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控规则版本';
