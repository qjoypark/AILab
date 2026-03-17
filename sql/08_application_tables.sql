-- 领用申请单表
CREATE TABLE material_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请单ID',
    application_no VARCHAR(50) NOT NULL UNIQUE COMMENT '申请单号',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    applicant_name VARCHAR(50) NOT NULL COMMENT '申请人姓名',
    applicant_dept VARCHAR(100) COMMENT '申请部门',
    application_type TINYINT NOT NULL COMMENT '申请类型:1-普通领用,2-危化品领用',
    usage_purpose VARCHAR(500) NOT NULL COMMENT '用途说明',
    usage_location VARCHAR(200) COMMENT '使用地点',
    expected_date DATE COMMENT '期望领用日期',
    status TINYINT DEFAULT 1 COMMENT '状态:1-待审批,2-审批中,3-审批通过,4-审批拒绝,5-已出库,6-已完成,7-已取消',
    approval_status TINYINT DEFAULT 0 COMMENT '审批状态:0-未审批,1-审批中,2-审批通过,3-审批拒绝',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    remark TEXT COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_application_no (application_no),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_status (status),
    INDEX idx_approval_status (approval_status),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领用申请单表';

-- 领用申请明细表
CREATE TABLE material_application_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    application_id BIGINT NOT NULL COMMENT '申请单ID',
    material_id BIGINT NOT NULL COMMENT '药品ID',
    material_name VARCHAR(200) NOT NULL COMMENT '药品名称',
    specification VARCHAR(100) COMMENT '规格',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    apply_quantity DECIMAL(10,2) NOT NULL COMMENT '申请数量',
    approved_quantity DECIMAL(10,2) COMMENT '批准数量',
    actual_quantity DECIMAL(10,2) COMMENT '实际出库数量',
    remark VARCHAR(500) COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_application_id (application_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领用申请明细表';
