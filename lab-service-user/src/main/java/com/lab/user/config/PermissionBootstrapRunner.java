package com.lab.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动时补齐核心权限并进行默认角色绑定。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionBootstrapRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 所有角色默认拥有的基础权限（按产品策略：所有人可进行申请与审批）。
     */
    private static final List<String> FOR_ALL_ROLE_PERMISSION_CODES = List.of(
            "application:list",
            "application:approve"
    );

    private static final List<String> LAB_USAGE_APPLICANT_PERMISSION_CODES = List.of(
            "module:lab",
            "lab-room:list",
            "lab-usage:list",
            "lab-usage:create",
            "lab-usage:cancel",
            "lab-usage:schedule:view"
    );

    private static final List<String> LAB_USAGE_APPROVER_PERMISSION_CODES = List.of(
            "module:lab",
            "lab-room:list",
            "lab-usage:list",
            "lab-usage:approve",
            "lab-usage:schedule:view"
    );

    private static final Map<String, List<String>> ROLE_DEFAULT_PERMISSION_CODES = Map.ofEntries(
            Map.entry("TEACHER", LAB_USAGE_APPLICANT_PERMISSION_CODES),
            Map.entry("006", LAB_USAGE_APPLICANT_PERMISSION_CODES),
            Map.entry("LAB_ROOM_MANAGER", LAB_USAGE_APPROVER_PERMISSION_CODES),
            Map.entry("LAB_MANAGER", LAB_USAGE_APPROVER_PERMISSION_CODES),
            Map.entry("005", LAB_USAGE_APPROVER_PERMISSION_CODES),
            Map.entry("CENTER_DIRECTOR", LAB_USAGE_APPROVER_PERMISSION_CODES),
            Map.entry("CENTER_ADMIN", LAB_USAGE_APPROVER_PERMISSION_CODES),
            Map.entry("003", LAB_USAGE_APPROVER_PERMISSION_CODES),
            Map.entry("DEPUTY_DEAN", LAB_USAGE_APPROVER_PERMISSION_CODES),
            Map.entry("002", LAB_USAGE_APPROVER_PERMISSION_CODES),
            Map.entry("DEAN", LAB_USAGE_APPROVER_PERMISSION_CODES),
            Map.entry("001", LAB_USAGE_APPROVER_PERMISSION_CODES)
    );

    private static final List<PermissionSeed> CORE_PERMISSIONS = List.of(
            // 系统管理
            new PermissionSeed("module:system", "系统管理", 1, null, 100),
            new PermissionSeed("system:user:list", "用户列表查看", 3, "module:system", 1010),
            new PermissionSeed("system:user:create", "用户创建", 3, "module:system", 1020),
            new PermissionSeed("system:user:update", "用户编辑", 3, "module:system", 1030),
            new PermissionSeed("system:user:delete", "用户删除", 3, "module:system", 1040),
            new PermissionSeed("system:user:assign-role", "用户角色分配", 3, "module:system", 1050),
            new PermissionSeed("system:role:list", "角色列表查看", 3, "module:system", 2010),
            new PermissionSeed("system:role:create", "角色创建", 3, "module:system", 2020),
            new PermissionSeed("system:role:update", "角色编辑", 3, "module:system", 2030),
            new PermissionSeed("system:role:delete", "角色删除", 3, "module:system", 2040),
            new PermissionSeed("system:role:assign-permission", "角色权限分配", 3, "module:system", 2050),

            // 物资管理
            new PermissionSeed("module:material", "物资管理", 1, null, 300),
            new PermissionSeed("material:list", "物资读取", 3, "module:material", 3010),
            new PermissionSeed("material:create", "物资新增", 3, "module:material", 3020),
            new PermissionSeed("material:update", "物资编辑", 3, "module:material", 3030),
            new PermissionSeed("material:delete", "物资删除", 3, "module:material", 3040),

            // 库存管理
            new PermissionSeed("module:inventory", "库存管理", 1, null, 350),
            new PermissionSeed("inventory:stock:list", "库存查询", 3, "module:inventory", 3510),
            new PermissionSeed("inventory:stock-in:list", "入库管理查看", 3, "module:inventory", 3520),
            new PermissionSeed("inventory:stock-in:create", "入库单新增", 3, "module:inventory", 3521),
            new PermissionSeed("inventory:stock-in:confirm", "入库单确认", 3, "module:inventory", 3522),
            new PermissionSeed("inventory:stock-in:delete", "入库单删除", 3, "module:inventory", 3523),
            new PermissionSeed("inventory:stock-out:list", "出库管理查看", 3, "module:inventory", 3530),
            new PermissionSeed("inventory:stock-out:create", "出库单新增", 3, "module:inventory", 3531),
            new PermissionSeed("inventory:stock-out:confirm", "出库单确认", 3, "module:inventory", 3532),
            new PermissionSeed("inventory:stock-out:delete", "出库单删除", 3, "module:inventory", 3533),
            new PermissionSeed("inventory:stock-check:list", "库存盘点查看", 3, "module:inventory", 3540),
            new PermissionSeed("inventory:stock-check:create", "盘点单新建", 3, "module:inventory", 3541),
            new PermissionSeed("inventory:stock-check:record", "盘点录入", 3, "module:inventory", 3542),
            new PermissionSeed("inventory:stock-check:complete", "盘点完成", 3, "module:inventory", 3543),

            // 申请审批
            new PermissionSeed("module:approval", "申请审批", 1, null, 400),
            new PermissionSeed("application:list", "申请查看", 3, "module:approval", 4010),
            new PermissionSeed("application:approve", "申请审批", 3, "module:approval", 4020),

            // 危化品管理
            new PermissionSeed("module:hazardous", "危化品管理", 1, null, 500),
            new PermissionSeed("hazardous:usage:list", "危化品使用记录查看", 3, "module:hazardous", 5010),
            new PermissionSeed("hazardous:ledger:view", "危化品台账查看", 3, "module:hazardous", 5020),

            // Lab management
            new PermissionSeed("module:lab", "Lab management", 1, null, 700),
            new PermissionSeed("lab-room:list", "Lab room list", 3, "module:lab", 7010),
            new PermissionSeed("lab-room:create", "Create lab room", 3, "module:lab", 7020),
            new PermissionSeed("lab-room:update", "Update lab room", 3, "module:lab", 7030),
            new PermissionSeed("lab-room:delete", "Delete lab room", 3, "module:lab", 7040),
            new PermissionSeed("lab-room:manager:update", "Update lab room managers", 3, "module:lab", 7050),
            new PermissionSeed("lab-usage:list", "Lab usage application list", 3, "module:lab", 7060),
            new PermissionSeed("lab-usage:create", "Create lab usage application", 3, "module:lab", 7070),
            new PermissionSeed("lab-usage:cancel", "Cancel lab usage application", 3, "module:lab", 7080),
            new PermissionSeed("lab-usage:approve", "Approve lab usage application", 3, "module:lab", 7090),
            new PermissionSeed("lab-usage:schedule:view", "View lab usage schedule", 3, "module:lab", 7100),

            // 预警管理
            new PermissionSeed("module:alert", "预警管理", 1, null, 600),
            new PermissionSeed("alert:list", "预警列表查看", 3, "module:alert", 6010)
    );

    @Override
    public void run(ApplicationArguments args) {
        try {
            Map<String, Long> permissionIdMap = ensurePermissions();
            bindAdminRole(permissionIdMap);
            bindDefaultPermissionsToAllRoles(permissionIdMap);
            bindRoleDefaultPermissions(permissionIdMap);
            log.info("Core permission bootstrap completed, size={}", permissionIdMap.size());
        } catch (Exception ex) {
            log.error("Failed to bootstrap core permissions", ex);
        }
    }

    private Map<String, Long> ensurePermissions() {
        Map<String, Long> codeIdMap = new LinkedHashMap<>();

        for (PermissionSeed seed : CORE_PERMISSIONS) {
            Long parentId = 0L;
            if (seed.parentCode() != null) {
                parentId = codeIdMap.getOrDefault(seed.parentCode(), 0L);
            }

            Long permissionId = jdbcTemplate.query(
                    "SELECT id FROM sys_permission WHERE permission_code = ? LIMIT 1",
                    rs -> rs.next() ? rs.getLong("id") : null,
                    seed.permissionCode()
            );

            if (permissionId == null) {
                jdbcTemplate.update(
                        "INSERT INTO sys_permission " +
                                "(permission_code, permission_name, permission_type, parent_id, sort_order, status) " +
                                "VALUES (?, ?, ?, ?, ?, 1)",
                        seed.permissionCode(),
                        seed.permissionName(),
                        seed.permissionType(),
                        parentId,
                        seed.sortOrder()
                );
                permissionId = jdbcTemplate.query(
                        "SELECT id FROM sys_permission WHERE permission_code = ? LIMIT 1",
                        rs -> rs.next() ? rs.getLong("id") : null,
                        seed.permissionCode()
                );
            } else {
                jdbcTemplate.update(
                        "UPDATE sys_permission SET permission_name = ?, permission_type = ?, parent_id = ?, sort_order = ?, status = 1 " +
                                "WHERE id = ?",
                        seed.permissionName(),
                        seed.permissionType(),
                        parentId,
                        seed.sortOrder(),
                        permissionId
                );
            }

            if (permissionId != null) {
                codeIdMap.put(seed.permissionCode(), permissionId);
            }
        }

        return codeIdMap;
    }

    private void bindAdminRole(Map<String, Long> permissionIdMap) {
        Long adminRoleId = jdbcTemplate.query(
                "SELECT id FROM sys_role WHERE role_code = 'ADMIN' AND deleted = 0 LIMIT 1",
                rs -> rs.next() ? rs.getLong("id") : null
        );
        if (adminRoleId == null) {
            log.warn("ADMIN role not found, skip binding permissions");
            return;
        }

        for (Long permissionId : permissionIdMap.values()) {
            insertRolePermissionIfAbsent(adminRoleId, permissionId);
        }
    }

    private void bindDefaultPermissionsToAllRoles(Map<String, Long> permissionIdMap) {
        List<Long> roleIds = jdbcTemplate.query(
                "SELECT id FROM sys_role WHERE deleted = 0",
                (rs, rowNum) -> rs.getLong("id")
        );
        if (roleIds.isEmpty()) {
            return;
        }

        for (String permissionCode : FOR_ALL_ROLE_PERMISSION_CODES) {
            Long permissionId = permissionIdMap.get(permissionCode);
            if (permissionId == null) {
                log.warn("Default permission code not found: {}", permissionCode);
                continue;
            }
            for (Long roleId : roleIds) {
                insertRolePermissionIfAbsent(roleId, permissionId);
            }
        }
    }

    private void bindRoleDefaultPermissions(Map<String, Long> permissionIdMap) {
        List<RoleSeed> roles = jdbcTemplate.query(
                "SELECT id, role_code FROM sys_role WHERE deleted = 0",
                (rs, rowNum) -> new RoleSeed(rs.getLong("id"), rs.getString("role_code"))
        );
        for (RoleSeed role : roles) {
            List<String> permissionCodes = ROLE_DEFAULT_PERMISSION_CODES.get(normalizeRoleCode(role.roleCode()));
            if (permissionCodes == null || permissionCodes.isEmpty()) {
                continue;
            }
            for (String permissionCode : permissionCodes) {
                Long permissionId = permissionIdMap.get(permissionCode);
                if (permissionId == null) {
                    log.warn("Role default permission code not found: role={}, permission={}", role.roleCode(), permissionCode);
                    continue;
                }
                insertRolePermissionIfAbsent(role.id(), permissionId);
            }
        }
    }

    private void insertRolePermissionIfAbsent(Long roleId, Long permissionId) {
        jdbcTemplate.update(
                "INSERT INTO sys_role_permission (role_id, permission_id) " +
                        "SELECT ?, ? FROM DUAL WHERE NOT EXISTS (" +
                        "SELECT 1 FROM sys_role_permission WHERE role_id = ? AND permission_id = ?)",
                roleId, permissionId, roleId, permissionId
        );
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode == null ? "" : roleCode.trim().toUpperCase();
    }

    private record PermissionSeed(
            String permissionCode,
            String permissionName,
            Integer permissionType,
            String parentCode,
            Integer sortOrder
    ) {
    }

    private record RoleSeed(Long id, String roleCode) {
    }
}
