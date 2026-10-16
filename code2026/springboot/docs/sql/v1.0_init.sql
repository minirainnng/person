-- 个人知识管理系统 V1.0 主表
-- MySQL 8.0+；数据库名应为 person
-- 部署到 Railway / Render 等云端时，先在数据库里执行本文件，再依次执行 v1.1 / v1.2

USE `person`;

-- ============================================
-- 用户表（普通用户）
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT '登录密码（建议 BCrypt）',
    `name` VARCHAR(50) NULL COMMENT '昵称',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色标识',
    `account` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额（保留字段）',
    `avatar` VARCHAR(500) NULL COMMENT '头像URL',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通用户表';

-- ============================================
-- 管理员表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT '登录密码',
    `name` VARCHAR(50) NULL COMMENT '姓名',
    `avatar` VARCHAR(500) NULL COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'ADMIN' COMMENT '角色标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ============================================
-- 文件夹表（支持多级）
-- ============================================
CREATE TABLE IF NOT EXISTS `folders` (
    `id` VARCHAR(64) NOT NULL COMMENT '文件夹ID（可用雪花/UUID）',
    `parent_id` VARCHAR(64) NULL DEFAULT NULL COMMENT '父文件夹ID，根目录为 NULL',
    `name` VARCHAR(100) NOT NULL COMMENT '文件夹名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_folders_parent_id` (`parent_id`),
    KEY `idx_folders_sort_order` (`sort_order`),
    KEY `idx_folders_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件夹表';

-- ============================================
-- 收藏表（核心表）
-- ============================================
CREATE TABLE IF NOT EXISTS `memories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `title` VARCHAR(255) NULL COMMENT '用户自定义标题',
    `source_title` VARCHAR(255) NULL COMMENT '来源原始标题',
    `source_type` VARCHAR(32) NULL COMMENT '类型：video / web / image / text',
    `original_url` VARCHAR(1000) NULL COMMENT '原始链接',
    `original_text` LONGTEXT NULL COMMENT '正文/提取内容',
    `image_path` VARCHAR(500) NULL COMMENT '图片存储路径',
    `note` TEXT NULL COMMENT '用户主观备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_memories_title` (`title`),
    KEY `idx_memories_source_type` (`source_type`),
    KEY `idx_memories_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ============================================
-- 收藏-文件夹关联表
-- ============================================
CREATE TABLE IF NOT EXISTS `memory_folders` (
    `id` VARCHAR(64) NOT NULL COMMENT '关联ID',
    `memory_id` BIGINT NOT NULL COMMENT '收藏ID',
    `folder_id` VARCHAR(64) NOT NULL COMMENT '文件夹ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_memory_folders_memory_id` (`memory_id`),
    KEY `idx_memory_folders_folder_id` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏-文件夹关联表';

-- ============================================
-- 初始管理员账号（密码：admin / 123456）
-- ⚠️ 部署到生产后请立即修改密码
-- 这里是明文密码占位，登录后请用管理端改密
-- ============================================
INSERT INTO `admin` (`username`, `password`, `name`, `role`)
VALUES ('admin', '123456', '超级管理员', 'ADMIN')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- ============================================
-- 初始普通用户（密码：user / 123456）
-- ⚠️ 部署到生产后请立即修改
-- ============================================
INSERT INTO `user` (`username`, `password`, `name`, `role`, `account`)
VALUES ('user', '123456', '演示用户', 'USER', 0.00)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
