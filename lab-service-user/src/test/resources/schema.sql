-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    user_type TINYINT NOT NULL,
    department VARCHAR(100),
    status TINYINT DEFAULT 1,
    safety_cert_status TINYINT DEFAULT 0,
    safety_cert_expire_date DATE,
    created_by BIGINT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_username_deleted (username, deleted)
);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    status TINYINT DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, role_id)
);

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    permission_type TINYINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(200),
    component VARCHAR(200),
    icon VARCHAR(100),
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (role_id, permission_id)
);

-- 审计日志表
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    real_name VARCHAR(50),
    operation_type VARCHAR(50) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    business_id BIGINT,
    operation_desc VARCHAR(500) NOT NULL,
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    request_params TEXT,
    response_result TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    operation_time TIMESTAMP NOT NULL,
    execution_time INT,
    status TINYINT DEFAULT 1,
    error_message TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试角色数据
INSERT INTO sys_role (role_code, role_name, description, status) VALUES
('ADMIN', '管理员', '系统管理员角色', 1),
('TEACHER', '教师', '教师角色', 1),
('STUDENT', '学生', '学生角色', 1);

-- 插入测试权限数据
INSERT INTO sys_permission (permission_code, permission_name, permission_type, status) VALUES
('system:user:view', '查看用户', 3, 1),
('system:user:create', '创建用户', 3, 1),
('system:user:update', '更新用户', 3, 1),
('system:user:delete', '删除用户', 3, 1),
('material:view', '查看药品', 3, 1),
('material:create', '创建药品', 3, 1),
('material:update', '更新药品', 3, 1),
('material:delete', '删除药品', 3, 1),
('inventory:view', '查看库存', 3, 1),
('inventory:manage', '管理库存', 3, 1);

-- 为角色分配权限
-- 管理员拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 1, id FROM sys_permission WHERE status = 1;

-- 教师拥有部分权限
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 2, id FROM sys_permission WHERE permission_code IN ('material:view', 'inventory:view', 'inventory:manage');

-- 学生只有查看权限
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 3, id FROM sys_permission WHERE permission_code IN ('material:view', 'inventory:view');
