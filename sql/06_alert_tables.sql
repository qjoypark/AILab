-- 库存预警配置表
CREATE TABLE stock_alert_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    material_id BIGINT NOT NULL COMMENT '药品ID',
    alert_type TINYINT NOT NULL COMMENT '预警类型:1-低库存,2-有效期,3-异常消耗',
    threshold_value DECIMAL(10,2) COMMENT '阈值',
    alert_days INT COMMENT '提前预警天数',
    status TINYINT DEFAULT 1 COMMENT '状态:0-停用,1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_material_alert (material_id, alert_type),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预警配置表';

-- 预警记录表
CREATE TABLE alert_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    alert_type TINYINT NOT NULL COMMENT '预警类型:1-低库存,2-有效期,3-异常消耗,4-账实差异,5-资质过期',
    alert_level TINYINT NOT NULL COMMENT '预警级别:1-提示,2-警告,3-严重',
    business_type VARCHAR(50) NOT NULL COMMENT '业务类型',
    business_id BIGINT COMMENT '业务ID',
    alert_title VARCHAR(200) NOT NULL COMMENT '预警标题',
    alert_content TEXT NOT NULL COMMENT '预警内容',
    alert_time DATETIME NOT NULL COMMENT '预警时间',
    status TINYINT DEFAULT 1 COMMENT '状态:1-未处理,2-已处理,3-已忽略',
    handler_id BIGINT COMMENT '处理人ID',
    handle_time DATETIME COMMENT '处理时间',
    handle_remark TEXT COMMENT '处理说明',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_alert_type (alert_type),
    INDEX idx_alert_level (alert_level),
    INDEX idx_status (status),
    INDEX idx_alert_time (alert_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';
