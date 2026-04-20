USE `xiaozhi`;

CREATE TABLE IF NOT EXISTS `xiaozhi`.`sys_knowledge_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `museum_id` BIGINT NOT NULL COMMENT '博物馆ID',
  `exhibit_id` BIGINT NULL COMMENT '展品ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `file_hash` VARCHAR(64) NOT NULL COMMENT '文件哈希',
  `file_size` BIGINT NULL COMMENT '文件大小',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/COMPLETED/FAILED',
  `chunk_count` INT NOT NULL DEFAULT 0 COMMENT '切片数',
  `error_msg` TEXT NULL COMMENT '失败信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_museum_hash` (`museum_id`, `file_hash`),
  KEY `idx_museum_status` (`museum_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

CREATE TABLE IF NOT EXISTS `xiaozhi`.`sys_knowledge_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `document_id` BIGINT NOT NULL COMMENT '文档ID',
  `chunk_index` INT NOT NULL COMMENT '切片序号',
  `content` MEDIUMTEXT NOT NULL COMMENT '切片内容',
  `vector_id` VARCHAR(128) NULL COMMENT '向量ID',
  `score_hint` DECIMAL(8,6) NULL COMMENT '检索得分',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_document_chunk` (`document_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库切片表';
