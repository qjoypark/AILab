-- 药品分类表
CREATE TABLE material_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类编码',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    category_level TINYINT NOT NULL COMMENT '分类层级:1-一级,2-二级,3-三级',
    sort_order INT DEFAULT 0 COMMENT '排序',
    description VARCHAR(500) COMMENT '分类描述',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_parent_id (parent_id),
    INDEX idx_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品分类表';

-- 药品信息表
CREATE TABLE material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '药品ID',
    material_code VARCHAR(50) NOT NULL UNIQUE COMMENT '药品编码',
    material_name VARCHAR(200) NOT NULL COMMENT '药品名称',
    material_type TINYINT NOT NULL COMMENT '药品类型:1-耗材,2-试剂,3-危化品',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    specification VARCHAR(100) COMMENT '规格型号',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    cas_number VARCHAR(50) COMMENT 'CAS号(化学品)',
    danger_category VARCHAR(100) COMMENT '危险类别',
    is_controlled TINYINT DEFAULT 0 COMMENT '是否管控:0-否,1-易制毒,2-易制爆',
    supplier_id BIGINT COMMENT '供应商ID',
    unit_price DECIMAL(10,2) COMMENT '单价',
    safety_stock INT DEFAULT 0 COMMENT '安全库存',
    max_stock INT COMMENT '最大库存',
    storage_condition VARCHAR(200) COMMENT '储存条件',
    shelf_life_days INT COMMENT '保质期(天)',
    description TEXT COMMENT '描述',
    image_url VARCHAR(500) COMMENT '图片URL',
    status TINYINT DEFAULT 1 COMMENT '状态:0-停用,1-启用',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_material_code (material_code),
    INDEX idx_material_type (material_type),
    INDEX idx_category_id (category_id),
    INDEX idx_is_controlled (is_controlled),
    INDEX idx_material_name (material_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品信息表';

-- 供应商表
CREATE TABLE supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '供应商ID',
    supplier_code VARCHAR(50) NOT NULL UNIQUE COMMENT '供应商编码',
    supplier_name VARCHAR(200) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    contact_email VARCHAR(100) COMMENT '联系邮箱',
    address VARCHAR(500) COMMENT '地址',
    qualification_file VARCHAR(500) COMMENT '资质文件URL',
    status TINYINT DEFAULT 1 COMMENT '状态:0-停用,1-启用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 插入初始分类数据
INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order, description) VALUES
('CONSUMABLE', '耗材', 0, 1, 1, '实验室消耗性物资'),
('REAGENT', '试剂', 0, 1, 2, '实验室化学试剂'),
('HAZARDOUS', '危化品', 0, 1, 3, '危险化学品');

-- 插入耗材子分类
INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order, description) 
SELECT 'CONSUMABLE_GLASS', '玻璃器皿', id, 2, 1, '各类玻璃实验器皿' FROM material_category WHERE category_code = 'CONSUMABLE';

INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order, description) 
SELECT 'CONSUMABLE_PLASTIC', '塑料耗材', id, 2, 2, '各类塑料实验耗材' FROM material_category WHERE category_code = 'CONSUMABLE';

-- 插入试剂子分类
INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order, description) 
SELECT 'REAGENT_ORGANIC', '有机试剂', id, 2, 1, '有机化学试剂' FROM material_category WHERE category_code = 'REAGENT';

INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order, description) 
SELECT 'REAGENT_INORGANIC', '无机试剂', id, 2, 2, '无机化学试剂' FROM material_category WHERE category_code = 'REAGENT';

-- 插入危化品子分类
INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order, description) 
SELECT 'HAZARDOUS_FLAMMABLE', '易燃品', id, 2, 1, '易燃危险化学品' FROM material_category WHERE category_code = 'HAZARDOUS';

INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order, description) 
SELECT 'HAZARDOUS_TOXIC', '有毒品', id, 2, 2, '有毒危险化学品' FROM material_category WHERE category_code = 'HAZARDOUS';

INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order, description) 
SELECT 'HAZARDOUS_CORROSIVE', '腐蚀品', id, 2, 3, '腐蚀性危险化学品' FROM material_category WHERE category_code = 'HAZARDOUS';
