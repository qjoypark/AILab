-- 出库单明细表
CREATE TABLE stock_out_detail (
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
CREATE TABLE stock_check (
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
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_check_no (check_no),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_check_date (check_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点表';

-- 库存盘点明细表
CREATE TABLE stock_check_detail (
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
