-- schema.sql
CREATE TABLE IF NOT EXISTS app_credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_id VARCHAR(32) NOT NULL UNIQUE,
    app_secret VARCHAR(64) NOT NULL,
    app_name VARCHAR(100),
    description VARCHAR(500),
    status INT DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 如果需要添加注释，使用H2的语法
COMMENT ON TABLE app_credentials IS '应用凭证表';
COMMENT ON COLUMN app_credentials.id IS '主键ID';
COMMENT ON COLUMN app_credentials.app_id IS '应用ID';
COMMENT ON COLUMN app_credentials.app_secret IS '应用密钥';
COMMENT ON COLUMN app_credentials.app_name IS '应用名称';
COMMENT ON COLUMN app_credentials.description IS '应用描述';
COMMENT ON COLUMN app_credentials.status IS '状态：1启用，0禁用';
COMMENT ON COLUMN app_credentials.created_time IS '创建时间';
COMMENT ON COLUMN app_credentials.updated_time IS '更新时间';