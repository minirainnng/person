-- 个人知识管理系统 V1.0 标签扩展
-- MySQL 8.0+；只创建标签和收藏-标签关联表，不写入测试数据。

-- 如果原表存在 DEFAULT '' 的时间定义，建议先执行：
ALTER TABLE `memory_folders`
    MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE `folders`
    MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 用户维护的标签。AI 后续提取的标签也先落到这里，用户确认后再建立关联。
CREATE TABLE IF NOT EXISTS `tags` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name` VARCHAR(100) NOT NULL COMMENT '标签名称，如 bjd、眼珠教程、配色',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tags_name` (`name`),
    KEY `idx_tags_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏标签表';

-- 收藏与标签多对多关联。一条收藏可以有多个标签，一个标签可以用于多个收藏。
CREATE TABLE IF NOT EXISTS `memory_tags` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `memory_id` BIGINT NOT NULL COMMENT '收藏ID，对应 memories.id',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID，对应 tags.id',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_tag` (`memory_id`, `tag_id`),
    KEY `idx_memory_tags_memory_id` (`memory_id`),
    KEY `idx_memory_tags_tag_id` (`tag_id`),
    CONSTRAINT `fk_memory_tags_memory` FOREIGN KEY (`memory_id`) REFERENCES `memories` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_memory_tags_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏与标签关联表';
