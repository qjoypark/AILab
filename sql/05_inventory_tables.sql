-- ============================================
-- 库存管理模块数据表
-- ============================================

-- 仓库表
CREATE TABLE IF NOT EXISTS warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '仓库ID',
    warehouse_code VARCHAR(50) NOT NULL UNIQUE COMMENT '仓库编码',
    warehouse_name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    warehouse_type TINYINT NOT NULL COMMENT '仓库类型:1-普通仓库,2-危化品仓库',
    location VARCHAR(200) COMMENT '位置',
    manager_id BIGINT COMMENT '负责人ID',
    status TINYINT DEFAULT 1 COMMENT '状态:0-停用,1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:0-未删除,1-已删除',
    INDEX idx_warehouse_code (warehouse_code),
    INDEX idx_warehouse_type (warehouse_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- 存储位置表
CREATE TABLE IF NOT EXISTS storage_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '位置ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    location_code VARCHAR(50) NOT NULL COMMENT '位置编码',
    location_name VARCHAR(100) NOT NULL COMMENT '位置名称',
    shelf_number VARCHAR(50) COMMENT '货架号',
    layer_number VARCHAR(50) COMMENT '层号',
    status TINYINT DEFAULT 1 COMMENT '状态:0-停用,1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:0-未删除,1-已删除',
    UNIQUE KEY uk_warehouse_location (warehouse_id, location_code),
    INDEX idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储位置表';

-- 库存表
CREATE TABLE IF NOT EXISTS stock_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存ID',
    material_id BIGINT NOT NULL COMMENT '药品ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    storage_location_id BIGINT COMMENT '存储位置ID',
    batch_number VARCHAR(50) COMMENT '批次号',
    quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '库存数量',
    available_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '可用数量',
    locked_quantity DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '锁定数量',
    production_date DATE COMMENT '生产日期',
    expire_date DATE COMMENT '有效期至',
    unit_price DECIMAL(10,2) COMMENT '单价',
    total_amount DECIMAL(12,2) COMMENT '总金额',
    last_check_date DATE COMMENT '最后盘点日期',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_material_warehouse_batch (material_id, warehouse_id, batch_number),
    INDEX idx_material_id (material_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_expire_date (expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 入库单表
CREATE TABLE IF NOT EXISTS stock_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '入库单ID',
    in_order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '入库单号',
    in_type TINYINT NOT NULL COMMENT '入库类型:1-采购入库,2-退货入库,3-盘盈入库,4-归还入库',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    supplier_id BIGINT COMMENT '供应商ID',
    total_amount DECIMAL(12,2) COMMENT '总金额',
    in_date DATE NOT NULL COMMENT '入库日期',
    operator_id BIGINT NOT NULL COMMENT '经手人ID',
    status TINYINT DEFAULT 1 COMMENT '状态:1-待入库,2-已入库,3-已取消',
    remark TEXT COMMENT '备注',
    attachment_url VARCHAR(500) COMMENT '附件URL',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:0-未删除,1-已删除',
    INDEX idx_in_order_no (in_order_no),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_in_date (in_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

-- 入库单明细表
CREATE TABLE IF NOT EXISTS stock_in_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    in_order_id BIGINT NOT NULL COMMENT '入库单ID',
    material_id BIGINT NOT NULL COMMENT '药品ID',
    batch_number VARCHAR(50) COMMENT '批次号',
    quantity DECIMAL(10,2) NOT NULL COMMENT '入库数量',
    unit_price DECIMAL(10,2) COMMENT '单价',
    total_amount DECIMAL(12,2) COMMENT '金额',
    production_date DATE COMMENT '生产日期',
    expire_date DATE COMMENT '有效期至',
    storage_location_id BIGINT COMMENT '存储位置ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_in_order_id (in_order_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

-- 出库单表
CREATE TABLE IF NOT EXISTS stock_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '出库单ID',
    out_order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '出库单号',
    out_type TINYINT NOT NULL COMMENT '出库类型:1-领用出库,2-报废出库,3-调拨出库',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    application_id BIGINT COMMENT '关联申请单ID',
    receiver_id BIGINT COMMENT '领用人ID',
    receiver_name VARCHAR(50) COMMENT '领用人姓名',
    receiver_dept VARCHAR(100) COMMENT '领用部门',
    out_date DATE NOT NULL COMMENT '出库日期',
    operator_id BIGINT NOT NULL COMMENT '经手人ID',
    status TINYINT DEFAULT 1 COMMENT '状态:1-待出库,2-已出库,3-已取消',
    remark TEXT COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:0-未删除,1-已删除',
    INDEX idx_out_order_no (out_order_no),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_application_id (application_id),
    INDEX idx_out_date (out_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单表';

-- 出库单明细表
CREATE TABLE IF NOT EXISTS stock_out_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    out_order_id BIGINT NOT NULL COMMENT '出库单ID',
    material_id BIGINT NOT NULL COMMENT '药品ID',
    batch_number VARCHAR(50) COMMENT '批次号',
    quantity DECIMAL(10,2) NOT NULL COMMENT '出库数量',
    unit_price DECIMAL(10,2) COMMENT '单价',
    total_amount DECIMAL(12,2) COMMENT '金额',
    storage_location_id BIGINT COMMENT '存储位置ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_out_order_id (out_order_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细表';

-- 库存盘点表
CREATE TABLE IF NOT EXISTS stock_check (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '盘点ID',
    check_no VARCHAR(50) NOT NULL UNIQUE COMMENT '盘点单号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    check_date DATE NOT NULL COMMENT '盘点日期',
    checker_id BIGINT NOT NULL COMMENT '盘点人ID',
    status TINYINT DEFAULT 1 COMMENT '状态:1-盘点中,2-已完成',
    remark TEXT COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:0-未删除,1-已删除',
    INDEX idx_check_no (check_no),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_check_date (check_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点表';

-- 库存盘点明细表
CREATE TABLE IF NOT EXISTS stock_check_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    check_id BIGINT NOT NULL COMMENT '盘点ID',
    material_id BIGINT NOT NULL COMMENT '药品ID',
    batch_number VARCHAR(50) COMMENT '批次号',
    book_quantity DECIMAL(10,2) NOT NULL COMMENT '账面数量',
    actual_quantity DECIMAL(10,2) NOT NULL COMMENT '实际数量',
    diff_quantity DECIMAL(10,2) NOT NULL COMMENT '差异数量',
    diff_reason VARCHAR(500) COMMENT '差异原因',
    storage_location_id BIGINT COMMENT '存储位置ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_check_id (check_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点明细表';

-- 插入初始仓库数据
INSERT INTO warehouse (warehouse_code, warehouse_name, warehouse_type, location, status) VALUES
('WH001', '主仓库', 1, '实验楼1层', 1),
('WH002', '危化品仓库', 2, '实验楼地下1层', 1);
