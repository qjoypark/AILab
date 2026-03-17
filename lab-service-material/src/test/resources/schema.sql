-- 药品分类表
CREATE TABLE material_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_code VARCHAR(50) NOT NULL UNIQUE,
    category_name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    category_level TINYINT NOT NULL,
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 药品信息表
CREATE TABLE material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(50) NOT NULL UNIQUE,
    material_name VARCHAR(200) NOT NULL,
    material_type TINYINT NOT NULL,
    category_id BIGINT NOT NULL,
    specification VARCHAR(100),
    unit VARCHAR(20) NOT NULL,
    cas_number VARCHAR(50),
    danger_category VARCHAR(100),
    is_controlled TINYINT DEFAULT 0,
    supplier_id BIGINT,
    unit_price DECIMAL(10,2),
    safety_stock INT DEFAULT 0,
    max_stock INT,
    storage_condition VARCHAR(200),
    shelf_life_days INT,
    description TEXT,
    image_url VARCHAR(500),
    status TINYINT DEFAULT 1,
    created_by BIGINT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 供应商表
CREATE TABLE supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_code VARCHAR(50) NOT NULL UNIQUE,
    supplier_name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    address VARCHAR(500),
    qualification_file VARCHAR(500),
    status TINYINT DEFAULT 1,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);
