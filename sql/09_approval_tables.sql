-- 审批流程配置表
CREATE TABLE approval_flow_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    flow_code VARCHAR(50) NOT NULL UNIQUE COMMENT '流程编码',
    flow_name VARCHAR(100) NOT NULL COMMENT '流程名称',
    business_type TINYINT NOT NULL COMMENT '业务类型:1-普通领用,2-危化品领用',
    flow_definition TEXT NOT NULL COMMENT '流程定义(JSON)',
    status TINYINT DEFAULT 1 COMMENT '状态:0-停用,1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_flow_code (flow_code),
    INDEX idx_business_type (business_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程配置表';

-- 审批记录表
CREATE TABLE approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    business_type INT DEFAULT 1 COMMENT 'business type:1-normal material,2-hazardous material,3-lab usage',
    business_no VARCHAR(50) COMMENT 'business order no',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    application_no VARCHAR(50) NOT NULL COMMENT '申请单号',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(50) NOT NULL COMMENT '审批人姓名',
    approval_level INT NOT NULL COMMENT '审批层级',
    approval_result TINYINT NOT NULL COMMENT '审批结果:1-通过,2-拒绝',
    approval_opinion TEXT COMMENT '审批意见',
    approval_time DATETIME NOT NULL COMMENT '审批时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_application_id (application_id),
    INDEX idx_business_application (business_type, application_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_approval_time (approval_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- 危化品使用记录表
CREATE TABLE hazardous_usage_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    material_id BIGINT NOT NULL COMMENT '药品ID',
    user_id BIGINT NOT NULL COMMENT '使用人ID',
    user_name VARCHAR(50) NOT NULL COMMENT '使用人姓名',
    received_quantity DECIMAL(10,2) NOT NULL COMMENT '领用数量',
    actual_used_quantity DECIMAL(10,2) COMMENT '实际使用数量',
    returned_quantity DECIMAL(10,2) COMMENT '归还数量',
    waste_quantity DECIMAL(10,2) COMMENT '废弃数量',
    usage_date DATE NOT NULL COMMENT '使用日期',
    return_date DATE COMMENT '归还日期',
    usage_location VARCHAR(200) COMMENT '使用地点',
    usage_purpose VARCHAR(500) COMMENT '使用目的',
    status TINYINT DEFAULT 1 COMMENT '状态:1-使用中,2-已归还,3-已完成',
    remark TEXT COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_application_id (application_id),
    INDEX idx_material_id (material_id),
    INDEX idx_user_id (user_id),
    INDEX idx_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危化品使用记录表';
