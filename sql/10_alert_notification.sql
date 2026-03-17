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

-- 消息通知表
CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    notification_type TINYINT NOT NULL COMMENT '通知类型:1-审批,2-预警,3-系统',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    business_type VARCHAR(50) COMMENT '业务类型',
    business_id BIGINT COMMENT '业务ID',
    push_channel TINYINT DEFAULT 1 COMMENT '推送渠道:1-站内,2-微信,3-短信,4-邮件',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读:0-未读,1-已读',
    read_time DATETIME COMMENT '阅读时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_is_read (is_read),
    INDEX idx_notification_type (notification_type),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';
