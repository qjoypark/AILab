-- 库存细粒度权限补齐（可重复执行）
-- 适用库：lab_management（MySQL）

START TRANSACTION;

-- 1) 补齐库存细粒度权限（缺失则插入）
INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'inventory:stock-in:create', '入库单新增', 3, 20, 3521, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'inventory:stock-in:create'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'inventory:stock-in:confirm', '入库单确认', 3, 20, 3522, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'inventory:stock-in:confirm'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'inventory:stock-in:delete', '入库单删除', 3, 20, 3523, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'inventory:stock-in:delete'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'inventory:stock-out:create', '出库单新增', 3, 20, 3531, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'inventory:stock-out:create'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'inventory:stock-out:confirm', '出库单确认', 3, 20, 3532, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'inventory:stock-out:confirm'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'inventory:stock-out:delete', '出库单删除', 3, 20, 3533, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'inventory:stock-out:delete'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'inventory:stock-check:create', '盘点单新建', 3, 20, 3541, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'inventory:stock-check:create'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'inventory:stock-check:record', '盘点录入', 3, 20, 3542, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'inventory:stock-check:record'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'inventory:stock-check:complete', '盘点完成', 3, 20, 3543, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'inventory:stock-check:complete'
);

-- 2) 为“已具备库存四个列表权限”的角色自动补齐库存动作权限
-- 说明：用于历史数据回填，保证此前“库存全权限角色”具备新增/确认/删除/录入/完成动作。
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT target_roles.role_id, target_permissions.permission_id
FROM (
  SELECT rp.role_id
  FROM sys_role_permission rp
  INNER JOIN sys_permission p ON p.id = rp.permission_id
  WHERE p.permission_code IN (
    'inventory:stock:list',
    'inventory:stock-in:list',
    'inventory:stock-out:list',
    'inventory:stock-check:list'
  )
  GROUP BY rp.role_id
  HAVING COUNT(DISTINCT p.permission_code) = 4
) AS target_roles
INNER JOIN (
  SELECT id AS permission_id
  FROM sys_permission
  WHERE permission_code IN (
    'inventory:stock-in:create',
    'inventory:stock-in:confirm',
    'inventory:stock-in:delete',
    'inventory:stock-out:create',
    'inventory:stock-out:confirm',
    'inventory:stock-out:delete',
    'inventory:stock-check:create',
    'inventory:stock-check:record',
    'inventory:stock-check:complete'
  )
) AS target_permissions
LEFT JOIN sys_role_permission rp_exists
  ON rp_exists.role_id = target_roles.role_id
 AND rp_exists.permission_id = target_permissions.permission_id
WHERE rp_exists.id IS NULL;

COMMIT;

