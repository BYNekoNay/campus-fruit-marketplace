-- V4__create_merchant_documents_table.sql
-- 商家证件材料表：存储营业执照、身份证等扫描件（MinIO 对象路径）

CREATE TABLE merchant_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL COMMENT '所属商家 ID',
    doc_type VARCHAR(50) NOT NULL COMMENT '证件类型：LICENSE/ID_CARD/STORE_PHOTO/OTHER',
    file_name VARCHAR(255) COMMENT '原始文件名',
    file_path VARCHAR(500) COMMENT 'MinIO 对象路径',
    file_size BIGINT COMMENT '文件大小（字节）',
    mime_type VARCHAR(100) COMMENT '文件 MIME 类型',
    scan_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '扫描状态：PENDING/CLEAN/INFECTED/ERROR',
    scan_result TEXT COMMENT '扫描结果详情',
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_doc_type (doc_type),
    CONSTRAINT fk_documents_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家证件材料表';
