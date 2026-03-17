-- 测试数据库表结构

-- 仓库表
CREATE TABLE IF NOT EXISTS warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_code VARCHAR(50) NOT NULL UNIQUE,
    warehouse_name VARCHAR(100) NOT NULL,
    warehouse_type TINYINT NOT NULL,
    location VARCHAR(200),
    manager_id BIGINT,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 存储位置表
CREATE TABLE IF NOT EXISTS storage_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    location_code VARCHAR(50) NOT NULL,
    location_name VARCHAR(100) NOT NULL,
    shelf_number VARCHAR(50),
    layer_number VARCHAR(50),
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 库存表
CREATE TABLE IF NOT EXISTS stock_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    storage_location_id BIGINT,
    batch_number VARCHAR(50),
    quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    available_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    locked_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    production_date DATE,
    expire_date DATE,
    unit_price DECIMAL(10,2),
    total_amount DECIMAL(12,2),
    last_check_date DATE,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 入库单表
CREATE TABLE IF NOT EXISTS stock_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    in_order_no VARCHAR(50) NOT NULL UNIQUE,
    in_type TINYINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    supplier_id BIGINT,
    total_amount DECIMAL(12,2),
    in_date DATE NOT NULL,
    operator_id BIGINT NOT NULL,
    status TINYINT DEFAULT 1,
    remark TEXT,
    attachment_url VARCHAR(500),
    created_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 入库单明细表
CREATE TABLE IF NOT EXISTS stock_in_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    in_order_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    batch_number VARCHAR(50),
    quantity DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(10,2),
    total_amount DECIMAL(12,2),
    production_date DATE,
    expire_date DATE,
    storage_location_id BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 出库单表
CREATE TABLE IF NOT EXISTS stock_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    out_order_no VARCHAR(50) NOT NULL UNIQUE,
    out_type TINYINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    application_id BIGINT,
    receiver_id BIGINT,
    receiver_name VARCHAR(50),
    receiver_dept VARCHAR(100),
    out_date DATE NOT NULL,
    operator_id BIGINT NOT NULL,
    status TINYINT DEFAULT 1,
    remark TEXT,
    created_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 出库单明细表
CREATE TABLE IF NOT EXISTS stock_out_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    out_order_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    batch_number VARCHAR(50),
    quantity DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(10,2),
    total_amount DECIMAL(12,2),
    storage_location_id BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 库存盘点表
CREATE TABLE IF NOT EXISTS stock_check (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_no VARCHAR(50) NOT NULL UNIQUE,
    warehouse_id BIGINT NOT NULL,
    check_date DATE NOT NULL,
    checker_id BIGINT NOT NULL,
    status TINYINT DEFAULT 1,
    remark TEXT,
    created_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 库存盘点明细表
CREATE TABLE IF NOT EXISTS stock_check_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    check_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    batch_number VARCHAR(50),
    book_quantity DECIMAL(10,2) NOT NULL,
    actual_quantity DECIMAL(10,2) NOT NULL,
    diff_quantity DECIMAL(10,2) NOT NULL,
    diff_reason VARCHAR(500),
    storage_location_id BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 库存预警配置表
CREATE TABLE IF NOT EXISTS stock_alert_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    alert_type TINYINT NOT NULL,
    threshold_value DECIMAL(10,2),
    alert_days INT,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 预警记录表
CREATE TABLE IF NOT EXISTS alert_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_type TINYINT NOT NULL,
    alert_level TINYINT NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    business_id BIGINT,
    alert_title VARCHAR(200) NOT NULL,
    alert_content TEXT NOT NULL,
    alert_time DATETIME NOT NULL,
    status TINYINT DEFAULT 1,
    handler_id BIGINT,
    handle_time DATETIME,
    handle_remark TEXT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 通知表
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receiver_id BIGINT NOT NULL,
    notification_type TINYINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    business_type VARCHAR(50),
    business_id BIGINT,
    push_channel TINYINT NOT NULL,
    is_read TINYINT DEFAULT 0,
    read_time DATETIME,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 待办事项表
CREATE TABLE IF NOT EXISTS todo_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    todo_type TINYINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    business_type VARCHAR(50),
    business_id BIGINT,
    priority TINYINT DEFAULT 2,
    status TINYINT DEFAULT 1,
    deadline DATETIME,
    completed_time DATETIME,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 危化品台账表
CREATE TABLE IF NOT EXISTS hazardous_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    record_type TINYINT NOT NULL,
    business_type VARCHAR(50),
    business_id BIGINT,
    in_quantity DECIMAL(10,2) DEFAULT 0,
    out_quantity DECIMAL(10,2) DEFAULT 0,
    balance_quantity DECIMAL(10,2) NOT NULL,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(50),
    remark TEXT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 危化品差异记录表
CREATE TABLE IF NOT EXISTS hazardous_discrepancy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    check_date DATE NOT NULL,
    book_quantity DECIMAL(10,2) NOT NULL,
    actual_quantity DECIMAL(10,2) NOT NULL,
    diff_quantity DECIMAL(10,2) NOT NULL,
    diff_reason TEXT,
    checker_id BIGINT NOT NULL,
    status TINYINT DEFAULT 1,
    handler_id BIGINT,
    handle_time DATETIME,
    handle_result TEXT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
