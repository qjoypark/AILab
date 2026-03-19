-- user table
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'user id',
    username VARCHAR(50) NOT NULL COMMENT 'username',
    password VARCHAR(255) NOT NULL COMMENT 'encrypted password',
    real_name VARCHAR(50) NOT NULL COMMENT 'real name',
    phone VARCHAR(20) COMMENT 'phone',
    email VARCHAR(100) COMMENT 'email',
    user_type TINYINT NOT NULL COMMENT '1-admin,2-teacher,3-student',
    department VARCHAR(100) COMMENT 'department',
    status TINYINT DEFAULT 1 COMMENT '0-disabled,1-enabled',
    safety_cert_status TINYINT DEFAULT 0 COMMENT '0-unverified,1-verified',
    safety_cert_expire_date DATE COMMENT 'safety cert expire date',
    created_by BIGINT COMMENT 'created by',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_by BIGINT COMMENT 'updated by',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    deleted TINYINT DEFAULT 0 COMMENT '0-not deleted,1-deleted',
    UNIQUE KEY uk_username_deleted (username, deleted),
    INDEX idx_username (username),
    INDEX idx_user_type (user_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user table';

-- role table
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'role id',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'role code',
    role_name VARCHAR(50) NOT NULL COMMENT 'role name',
    description VARCHAR(200) COMMENT 'role description',
    status TINYINT DEFAULT 1 COMMENT '0-disabled,1-enabled',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    deleted TINYINT DEFAULT 0 COMMENT 'delete flag'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role table';

-- user-role relation table
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    user_id BIGINT NOT NULL COMMENT 'user id',
    role_id BIGINT NOT NULL COMMENT 'role id',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user role relation table';

-- permission table
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'permission id',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT 'permission code',
    permission_name VARCHAR(100) NOT NULL COMMENT 'permission name',
    permission_type TINYINT NOT NULL COMMENT '1-menu,2-button,3-api',
    parent_id BIGINT DEFAULT 0 COMMENT 'parent permission id',
    path VARCHAR(200) COMMENT 'route path',
    component VARCHAR(200) COMMENT 'component path',
    icon VARCHAR(100) COMMENT 'icon',
    sort_order INT DEFAULT 0 COMMENT 'sort order',
    status TINYINT DEFAULT 1 COMMENT '0-disabled,1-enabled',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='permission table';

-- role-permission relation table
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    role_id BIGINT NOT NULL COMMENT 'role id',
    permission_id BIGINT NOT NULL COMMENT 'permission id',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role permission relation table';
