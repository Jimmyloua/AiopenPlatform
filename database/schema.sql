-- AI Open Platform Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS ai_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_platform;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色: admin, user',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active, inactive, banned',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- API密钥表
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '密钥ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) COMMENT '密钥名称',
    key_hash VARCHAR(255) NOT NULL COMMENT 'API密钥哈希',
    key_prefix VARCHAR(10) NOT NULL COMMENT '密钥前缀(用于识别)',
    permissions JSON COMMENT '权限配置',
    rate_limit INT DEFAULT 100 COMMENT '速率限制(请求/分钟)',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    last_used_at TIMESTAMP NULL COMMENT '最后使用时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_key_prefix (key_prefix)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API密钥表';

-- 凭证表 (AK/SK管理 - 用于会议、助手和CUI后端)
CREATE TABLE IF NOT EXISTS credentials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '凭证ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) NOT NULL COMMENT '凭证名称',
    type VARCHAR(50) NOT NULL COMMENT '凭证类型: meeting(会议), assistant(助手), cui_backend(CUI后端)',
    access_key VARCHAR(100) NOT NULL UNIQUE COMMENT 'Access Key (AK) - 公开的访问标识',
    secret_key_hash VARCHAR(255) NOT NULL COMMENT 'Secret Key (SK) - 加密存储的密钥哈希',
    encryption_key_id VARCHAR(100) COMMENT 'SK加密密钥ID',
    resource_id BIGINT COMMENT '关联的资源ID (会议ID/助手ID/CUI后端ID)',
    permissions JSON COMMENT '权限范围 (JSON格式)',
    rate_limit INT DEFAULT 100 COMMENT '速率限制 (请求/分钟)',
    ip_whitelist JSON COMMENT 'IP白名单 (JSON数组)',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    last_used_at TIMESTAMP NULL COMMENT '最后使用时间',
    usage_count BIGINT DEFAULT 0 COMMENT '使用次数',
    description VARCHAR(500) COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_access_key (access_key),
    INDEX idx_resource_id (resource_id),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='凭证表(AK/SK管理)';

-- 连接配置表
CREATE TABLE IF NOT EXISTS connections (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '连接ID',
    name VARCHAR(100) NOT NULL COMMENT '连接名称',
    type ENUM('local', 'remote') NOT NULL COMMENT '连接类型',
    provider VARCHAR(50) COMMENT '提供商(远程连接)',
    endpoint VARCHAR(255) NOT NULL COMMENT '端点地址',
    credential_id BIGINT COMMENT '凭证ID',
    config JSON COMMENT '连接配置',
    health_status VARCHAR(20) DEFAULT 'unknown' COMMENT '健康状态: healthy, unhealthy, unknown',
    last_check_at TIMESTAMP NULL COMMENT '最后检查时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    FOREIGN KEY (credential_id) REFERENCES credentials(id),
    INDEX idx_type (type),
    INDEX idx_health_status (health_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连接配置表';

-- 技能表
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '技能ID',
    name VARCHAR(100) NOT NULL COMMENT '技能名称',
    description TEXT COMMENT '技能描述',
    category VARCHAR(50) COMMENT '技能分类',
    schema_json JSON NOT NULL COMMENT 'JSON Schema定义',
    handler_config JSON COMMENT '处理器配置',
    is_public BOOLEAN DEFAULT TRUE COMMENT '是否公开',
    created_by BIGINT COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_category (category),
    INDEX idx_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能表';

-- 助手表
CREATE TABLE IF NOT EXISTS assistants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '助手ID',
    name VARCHAR(100) NOT NULL COMMENT '助手名称',
    description TEXT COMMENT '助手描述',
    avatar VARCHAR(500) COMMENT '头像URL',
    system_prompt TEXT COMMENT '系统提示词',
    model_config JSON COMMENT '模型配置',
    capabilities JSON COMMENT '能力配置',
    is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开到广场',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态: draft, pending_review, published, rejected',
    review_comment TEXT COMMENT '审核意见',
    created_by BIGINT COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_is_public (is_public),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='助手表';

-- 助手技能关联表
CREATE TABLE IF NOT EXISTS assistant_skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    assistant_id BIGINT NOT NULL COMMENT '助手ID',
    skill_id BIGINT NOT NULL COMMENT '技能ID',
    config JSON COMMENT '技能配置',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (assistant_id) REFERENCES assistants(id),
    FOREIGN KEY (skill_id) REFERENCES skills(id),
    UNIQUE KEY uk_assistant_skill (assistant_id, skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='助手技能关联表';

-- 会话表
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    assistant_id BIGINT COMMENT '助手ID',
    title VARCHAR(255) COMMENT '会话标题',
    metadata JSON COMMENT '元数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (assistant_id) REFERENCES assistants(id),
    INDEX idx_user_id (user_id),
    INDEX idx_assistant_id (assistant_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role ENUM('user', 'assistant', 'system', 'tool') NOT NULL COMMENT '角色',
    content TEXT COMMENT '消息内容',
    tool_calls JSON COMMENT '工具调用',
    tool_call_id VARCHAR(100) COMMENT '工具调用ID',
    thinking_content TEXT COMMENT '思考内容',
    token_count INT COMMENT 'Token数量',
    metadata JSON COMMENT '元数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_role (role),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 会话缓存表(Redis持久化备份)
CREATE TABLE IF NOT EXISTS session_states (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '状态ID',
    session_id VARCHAR(100) NOT NULL UNIQUE COMMENT '会话标识',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    conversation_id BIGINT COMMENT '会话ID',
    state_data JSON COMMENT '状态数据',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    INDEX idx_session_id (session_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话状态表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    action VARCHAR(100) NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(50) COMMENT '资源类型',
    resource_id BIGINT COMMENT '资源ID',
    details JSON COMMENT '详细信息',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- 插入默认管理员账户 (密码: admin123)
INSERT INTO users (username, email, password_hash, nickname, role) VALUES
('admin', 'admin@aiplatform.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6vzKK', '系统管理员', 'admin');

-- 插入默认技能
INSERT INTO skills (name, description, category, schema_json, handler_config, is_public) VALUES
('网页搜索', '搜索互联网获取实时信息', 'search', '{"type": "object", "properties": {"query": {"type": "string", "description": "搜索关键词"}}}', '{"type": "http", "endpoint": "/api/tools/search"}', TRUE),
('代码执行', '在沙箱环境中执行代码', 'code', '{"type": "object", "properties": {"code": {"type": "string"}, "language": {"type": "string"}}}', '{"type": "sandbox", "timeout": 60}', TRUE),
('数据分析', '分析数据并生成图表', 'analysis', '{"type": "object", "properties": {"data": {"type": "string"}, "analysis_type": {"type": "string"}}}', '{"type": "http", "endpoint": "/api/tools/analysis"}', TRUE);

-- 插入默认助手
INSERT INTO assistants (name, description, system_prompt, model_config, capabilities, is_public, status, created_by) VALUES
('通用助手', '一个全能的AI助手，能够回答各种问题', '你是一个友好、专业的AI助手。请用清晰、准确的方式回答用户的问题。', '{"model": "gpt-4", "temperature": 0.7}', '["search", "analysis"]', TRUE, 'published', 1),
('编程助手', '专注于编程问题的AI助手', '你是一个专业的编程助手。帮助用户解决编程问题，提供代码示例和最佳实践建议。', '{"model": "gpt-4", "temperature": 0.5}', '["code", "search"]', TRUE, 'published', 1);