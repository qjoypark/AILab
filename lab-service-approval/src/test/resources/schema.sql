-- 领用申请单表
CREATE TABLE IF NOT EXISTS material_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_no VARCHAR(50) NOT NULL UNIQUE,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(50) NOT NULL,
    applicant_dept VARCHAR(100),
    application_type TINYINT NOT NULL,
    usage_purpose VARCHAR(500) NOT NULL,
    usage_location VARCHAR(200),
    expected_date DATE,
    status TINYINT DEFAULT 1,
    approval_status TINYINT DEFAULT 0,
    current_approver_id BIGINT,
    remark TEXT,
    created_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 领用申请明细表
CREATE TABLE IF NOT EXISTS material_application_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    material_name VARCHAR(200) NOT NULL,
    specification VARCHAR(100),
    unit VARCHAR(20) NOT NULL,
    apply_quantity DECIMAL(10,2) NOT NULL,
    approved_quantity DECIMAL(10,2),
    actual_quantity DECIMAL(10,2),
    remark VARCHAR(500),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 审批流程配置表
CREATE TABLE IF NOT EXISTS approval_flow_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_code VARCHAR(50) NOT NULL UNIQUE,
    flow_name VARCHAR(100) NOT NULL,
    business_type TINYINT NOT NULL,
    flow_definition TEXT NOT NULL,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 审批记录表
CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    application_no VARCHAR(50) NOT NULL,
    approver_id BIGINT NOT NULL,
    approver_name VARCHAR(50) NOT NULL,
    approval_level INT NOT NULL,
    approval_result TINYINT NOT NULL,
    approval_opinion TEXT,
    approval_time DATETIME NOT NULL,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
