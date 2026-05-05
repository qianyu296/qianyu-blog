-- 博客系统数据库初始化脚本
-- 执行方式: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS qianyu_blog
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE qianyu_blog;

-- 分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文章表
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    cover_image_url VARCHAR(500),
    category_id BIGINT NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_status (status),
    INDEX idx_category (category_id),
    INDEX idx_created (created_at),
    INDEX idx_pinned (is_pinned),
    CONSTRAINT fk_post_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(50) NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_post_id (post_id),
    INDEX idx_name (name),
    UNIQUE INDEX idx_post_slug (post_id, slug),
    CONSTRAINT fk_tag_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 管理员用户表
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS music_tracks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    artist VARCHAR(120),
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'calm',
    lyrics_content TEXT,
    duration_seconds INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS site_settings (
    id BIGINT PRIMARY KEY,
    site_name VARCHAR(120) NOT NULL,
    site_subtitle VARCHAR(255),
    hero_badge VARCHAR(120),
    hero_title VARCHAR(120) NOT NULL,
    hero_description VARCHAR(500) NOT NULL,
    avatar_image_url VARCHAR(500),
    hero_background_image_url VARCHAR(500),
    default_post_cover_url VARCHAR(500),
    github_url VARCHAR(500),
    email VARCHAR(255),
    footer_text VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS media_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    display_name VARCHAR(255),
    alt_text VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL,
    image_api_key VARCHAR(255),
    api_endpoint VARCHAR(500) NOT NULL,
    image_api_endpoint VARCHAR(500),
    model_name VARCHAR(120) NOT NULL,
    image_model_name VARCHAR(120) NOT NULL DEFAULT 'dall-e-3',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_tokens INT NOT NULL DEFAULT 4096,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 初始数据
INSERT INTO categories (name, slug, description, created_at, updated_at) VALUES
('技术', 'tech', '编程、开发、技术笔记', NOW(), NOW()),
('生活', 'life', '日常生活、随笔', NOW(), NOW()),
('读书', 'reading', '读书笔记、书评', NOW(), NOW());

-- 示例文章（标签和置顶示例）
INSERT INTO posts (title, summary, content, cover_image_url, category_id, is_pinned, status, created_at, updated_at) VALUES
('欢迎来到千语博客', '这是一个基于 Spring Boot + Vue 3 的博客系统，支持标签和 Markdown', '## 博客系统介绍\n\n这是一个前后端分离的博客系统，使用以下技术栈：\n\n- 后端：Spring Boot 3 + JPA + Spring Security\n- 前端：Vue 3 + TypeScript + Vite\n- 数据库：MySQL\n- 认证：JWT\n\n### 主要功能\n\n1. **标签系统** - 为文章添加标签\n2. **文章置顶** - 重要文章可以置顶显示\n3. **Markdown 编辑** - 支持 Markdown 编写\n\n欢迎大家来访！', NULL, 1, TRUE, 'PUBLISHED', NOW(), NOW()),
('Spring Boot 入门指南', '快速上手 Spring Boot 开发，从零开始构建应用', '## Spring Boot 入门\n\nSpring Boot 让创建独立的、生产级别的 Spring 应用变得非常简单。\n\n### 主要特性\n\n1. 自动配置\n2. 内嵌服务器\n3. 零配置\n\n### 快速开始\n\n```java\n@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}\n```', NULL, 1, FALSE, 'PUBLISHED', NOW(), NOW()),
('我的2024年计划', '新的一年，新的开始，制定学习目标', '## 年度计划\n\n今年计划完成以下目标：\n\n- 坚持每周写博客\n- 学习新技术\n- 读10本书\n\n### 学习方向\n\n1. 深入 Spring Boot\n2. 掌握 Vue 3 生态\n3. 了解云原生部署', NULL, 2, FALSE, 'PUBLISHED', NOW(), NOW());

-- 示例标签
INSERT INTO tags (name, slug, post_id, created_at) VALUES
('Java', 'java', 1, NOW()),
('Spring Boot', 'spring-boot', 1, NOW()),
('Vue', 'vue', 1, NOW()),
('Java', 'java', 2, NOW()),
('后端', '后端', 2, NOW()),
('随笔', '随笔', 3, NOW());

INSERT INTO site_settings (
    id, site_name, site_subtitle, hero_badge, hero_title, hero_description, avatar_image_url,
    hero_background_image_url, default_post_cover_url, github_url, email, footer_text, created_at, updated_at
) VALUES (
    1, '千语博客', '记录学习、开发和生活片段', '技术 · 生活 · 思考', '千语博客',
    '记录学习、开发和生活片段，在代码与文字间寻找平衡', NULL, NULL, NULL, NULL, NULL,
    '用代码和文字持续构建自己的内容站。', NOW(), NOW()
);
