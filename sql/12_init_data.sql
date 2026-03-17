-- 初始化角色数据
INSERT INTO sys_role (role_code, role_name, description) VALUES
('ADMIN', '系统管理员', '系统管理员，拥有所有权限'),
('CENTER_ADMIN', '中心管理员', '实验实训中心管理人员'),
('LAB_MANAGER', '实验室负责人', '实验室PI或负责人'),
('TEACHER', '任课教师', '实验课程授课教师'),
('STUDENT', '学生', '参与实验实训的学生'),
('EQUIPMENT_ADMIN', '设备管理员', '负责设备维护的人员');

-- 初始化管理员用户 (密码: admin123, 使用BCrypt加密)
INSERT INTO sys_user (username, password, real_name, user_type, department, status, safety_cert_status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 1, '信息中心', 1, 1);

-- 关联管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 初始化药品分类
INSERT INTO material_category (category_code, category_name, parent_id, category_level, sort_order) VALUES
('CONSUMABLE', '耗材', 0, 1, 1),
('REAGENT', '试剂', 0, 1, 2),
('HAZARDOUS', '危化品', 0, 1, 3);

-- 初始化仓库
INSERT INTO warehouse (warehouse_code, warehouse_name, warehouse_type, location, status) VALUES
('WH001', '普通仓库1号', 1, '实验楼1层', 1),
('WH002', '危化品仓库', 2, '实验楼地下1层', 1);

-- 初始化审批流程配置
INSERT INTO approval_flow_config (flow_code, flow_name, business_type, flow_definition, status) VALUES
('NORMAL_APPLY', '普通领用审批流程', 1, '{"levels":[{"level":1,"approverRole":"LAB_MANAGER","approverName":"实验室负责人"}]}', 1),
('HAZARDOUS_APPLY', '危化品领用审批流程', 2, '{"levels":[{"level":1,"approverRole":"LAB_MANAGER","approverName":"实验室负责人"},{"level":2,"approverRole":"CENTER_ADMIN","approverName":"中心管理员"},{"level":3,"approverRole":"ADMIN","approverName":"安全管理员"}]}', 1);
