-- SOP 文档与收藏引用关系
-- MariaDB / MySQL，当前数据库应为 person
USE `person`;

CREATE TABLE IF NOT EXISTS `sops` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SOP文档ID',
    `title` VARCHAR(255) NOT NULL COMMENT 'SOP文档标题',
    `content_html` LONGTEXT NULL COMMENT '富文本HTML正文',
    `content_text` LONGTEXT NULL COMMENT '纯文本正文，用于搜索和摘要',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sops_sort_order` (`sort_order`),
    KEY `idx_sops_updated_at` (`updated_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='SOP富文本文档表';

CREATE TABLE IF NOT EXISTS `sop_memory_refs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '引用关系ID',
    `sop_id` BIGINT NOT NULL COMMENT 'SOP文档ID，对应sops.id',
    `memory_id` BIGINT NULL COMMENT '收藏ID，对应memories.id；收藏删除后置空',
    `memory_title_snapshot` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '引用时的收藏标题快照',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '引用建立时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sop_memory` (`sop_id`, `memory_id`),
    KEY `idx_sop_memory_refs_memory_id` (`memory_id`),
    CONSTRAINT `fk_sop_memory_refs_sop`
        FOREIGN KEY (`sop_id`) REFERENCES `sops` (`id`)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_sop_memory_refs_memory`
        FOREIGN KEY (`memory_id`) REFERENCES `memories` (`id`)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='SOP与收藏引用关系表';
