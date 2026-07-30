CREATE TABLE canonical_fruits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(100) NOT NULL COMMENT '品类如"柑橘类"',
    variety VARCHAR(200) NOT NULL COMMENT '品种如"赣南脐橙"',
    grade VARCHAR(50) NOT NULL COMMENT '等级如"一级"',
    origin VARCHAR(200) COMMENT '产地',
    default_unit VARCHAR(20) DEFAULT 'g' COMMENT '默认计量单位',
    comparison_group_id BIGINT COMMENT '可比组ID',
    version INT DEFAULT 1 COMMENT '不可变版本',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_canonical_fruits_category (category),
    INDEX idx_canonical_fruits_comparison_group (comparison_group_id),
    INDEX idx_canonical_fruits_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标准水果目录';
