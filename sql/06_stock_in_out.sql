-- 入库单表
CREATE TABLE stock_in (
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
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_in_order_no (in_order_no),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_in_date (in_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

-- 入库单明细表
CREATE TABLE stock_in_detail (
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
CREATE TABLE stock_out (
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
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_out_order_no (out_order_no),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_application_id (application_id),
    INDEX idx_out_date (out_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单表';
