-- Lab usage approval workflow and permission backfill.
-- Target database: lab_management (MySQL).

START TRANSACTION;

SET @add_business_type_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'approval_record'
        AND column_name = 'business_type'
    ),
    'SELECT 1',
    'ALTER TABLE approval_record ADD COLUMN business_type INT DEFAULT 1 COMMENT ''business type:1-normal material,2-hazardous material,3-lab usage'' AFTER id'
  )
);
PREPARE add_business_type_stmt FROM @add_business_type_sql;
EXECUTE add_business_type_stmt;
DEALLOCATE PREPARE add_business_type_stmt;

SET @add_business_no_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'approval_record'
        AND column_name = 'business_no'
    ),
    'SELECT 1',
    'ALTER TABLE approval_record ADD COLUMN business_no VARCHAR(50) NULL COMMENT ''business order no'' AFTER business_type'
  )
);
PREPARE add_business_no_stmt FROM @add_business_no_sql;
EXECUTE add_business_no_stmt;
DEALLOCATE PREPARE add_business_no_stmt;

SET @add_business_index_sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'approval_record'
        AND index_name = 'idx_approval_record_business'
    ),
    'SELECT 1',
    'ALTER TABLE approval_record ADD INDEX idx_approval_record_business (business_type, application_id)'
  )
);
PREPARE add_business_index_stmt FROM @add_business_index_sql;
EXECUTE add_business_index_stmt;
DEALLOCATE PREPARE add_business_index_stmt;

UPDATE approval_record ar
INNER JOIN material_application ma ON ma.id = ar.application_id
SET ar.business_type = ma.application_type,
    ar.business_no = ma.application_no
WHERE (ar.business_type IS NULL OR ar.business_no IS NULL)
  AND ma.application_type IN (1, 2);

INSERT INTO approval_flow_config (flow_code, flow_name, business_type, flow_definition, status)
SELECT
  'LAB_USAGE_APPLY',
  'Lab usage approval flow',
  3,
  '{"levels":[{"level":1,"approverRole":"LAB_ROOM_MANAGER","approverName":"Lab room manager"},{"level":2,"approverRole":"CENTER_DIRECTOR","approverName":"Center director"},{"level":3,"approverRole":"DEPUTY_DEAN","approverName":"Deputy dean"},{"level":4,"approverRole":"DEAN","approverName":"Dean"}]}',
  1
WHERE NOT EXISTS (
  SELECT 1 FROM approval_flow_config WHERE flow_code = 'LAB_USAGE_APPLY'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT 'module:lab', 'Lab management', 1, 0, 700, 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE permission_code = 'module:lab'
);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status)
SELECT p.permission_code, p.permission_name, 3, module_permission.id, p.sort_order, 1
FROM (
  SELECT 'lab-room:list' AS permission_code, 'Lab room list' AS permission_name, 7010 AS sort_order
  UNION ALL SELECT 'lab-room:create', 'Create lab room', 7020
  UNION ALL SELECT 'lab-room:update', 'Update lab room', 7030
  UNION ALL SELECT 'lab-room:delete', 'Delete lab room', 7040
  UNION ALL SELECT 'lab-room:manager:update', 'Update lab room managers', 7050
  UNION ALL SELECT 'lab-usage:list', 'Lab usage application list', 7060
  UNION ALL SELECT 'lab-usage:create', 'Create lab usage application', 7070
  UNION ALL SELECT 'lab-usage:cancel', 'Cancel lab usage application', 7080
  UNION ALL SELECT 'lab-usage:approve', 'Approve lab usage application', 7090
  UNION ALL SELECT 'lab-usage:schedule:view', 'View lab usage schedule', 7100
) p
INNER JOIN sys_permission module_permission ON module_permission.permission_code = 'module:lab'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission exists_permission WHERE exists_permission.permission_code = p.permission_code
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
INNER JOIN sys_permission p ON p.permission_code IN (
  'module:lab',
  'lab-room:list',
  'lab-room:create',
  'lab-room:update',
  'lab-room:delete',
  'lab-room:manager:update',
  'lab-usage:list',
  'lab-usage:create',
  'lab-usage:cancel',
  'lab-usage:approve',
  'lab-usage:schedule:view'
)
LEFT JOIN sys_role_permission existing_rp
  ON existing_rp.role_id = r.id
 AND existing_rp.permission_id = p.id
WHERE r.role_code = 'ADMIN'
  AND existing_rp.id IS NULL;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
INNER JOIN sys_permission p ON p.permission_code IN (
  'module:lab',
  'lab-room:list',
  'lab-usage:list',
  'lab-usage:create',
  'lab-usage:cancel',
  'lab-usage:schedule:view'
)
LEFT JOIN sys_role_permission existing_rp
  ON existing_rp.role_id = r.id
 AND existing_rp.permission_id = p.id
WHERE r.role_code IN ('TEACHER', '006')
  AND existing_rp.id IS NULL;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
INNER JOIN sys_permission p ON p.permission_code IN (
  'module:lab',
  'lab-room:list',
  'lab-usage:list',
  'lab-usage:approve',
  'lab-usage:schedule:view'
)
LEFT JOIN sys_role_permission existing_rp
  ON existing_rp.role_id = r.id
 AND existing_rp.permission_id = p.id
WHERE r.role_code IN (
  'LAB_ROOM_MANAGER', '005', 'LAB_MANAGER',
  'CENTER_DIRECTOR', '003', 'CENTER_ADMIN',
  'DEPUTY_DEAN', '002',
  'DEAN', '001'
)
  AND existing_rp.id IS NULL;

COMMIT;
