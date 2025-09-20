-- MySQL数据库初始化脚本
-- 注意：请确保数据库openapi_service已创建

-- 创建应用凭证表（如果不存在）
CREATE TABLE IF NOT EXISTS app_credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    app_id VARCHAR(32) NOT NULL UNIQUE COMMENT '应用ID',
    app_secret VARCHAR(64) NOT NULL COMMENT '应用密钥',
    app_name VARCHAR(100) COMMENT '应用名称',
    description VARCHAR(500) COMMENT '应用描述',
    status INT DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用凭证表';

-- 插入测试数据（使用INSERT IGNORE避免重复插入）
INSERT IGNORE INTO app_credentials (app_id, app_secret, app_name, description, status) VALUES 
('test-app-001', 'test-secret-001-abcdef123456789', '测试应用', '用于API测试的默认应用', 1),
('demo-app-002', 'demo-secret-002-xyz987654321abc', '演示应用', '用于功能演示的应用', 1),
('prod-app-003', 'prod-secret-003-secure-long-key', '生产应用', '生产环境使用的应用', 1);
