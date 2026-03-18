package com.lab.user.property;

import com.lab.user.dto.LoginResponse;
import com.lab.user.mapper.SysUserMapper;
import com.lab.user.service.AuthService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Tag("Feature: smart-lab-management-system, Property: 多角色权限并集去重")
class PermissionUnionPropertyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void multiRolePermissionUnionShouldBeDistinctInMapperAndAuthView() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "union_user_" + suffix;
        String roleCodeA = "UNION_ROLE_A_" + suffix;
        String roleCodeB = "UNION_ROLE_B_" + suffix;
        String permissionShared = "perm:union:shared:" + suffix;
        String permissionA = "perm:union:a:" + suffix;
        String permissionB = "perm:union:b:" + suffix;

        Long userId = null;
        Long roleIdA = null;
        Long roleIdB = null;
        Long permissionIdShared = null;
        Long permissionIdA = null;
        Long permissionIdB = null;

        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_user (username, password, real_name, user_type, status, department) VALUES (?, ?, ?, ?, ?, ?)",
                    username,
                    passwordEncoder.encode("Test@123456"),
                    "权限并集测试用户",
                    2,
                    1,
                    "测试部门"
            );
            userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);

            jdbcTemplate.update(
                    "INSERT INTO sys_role (role_code, role_name, status, deleted) VALUES (?, ?, ?, ?)",
                    roleCodeA,
                    "并集角色A",
                    1,
                    0
            );
            jdbcTemplate.update(
                    "INSERT INTO sys_role (role_code, role_name, status, deleted) VALUES (?, ?, ?, ?)",
                    roleCodeB,
                    "并集角色B",
                    1,
                    0
            );
            roleIdA = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCodeA);
            roleIdB = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCodeB);

            permissionIdShared = insertPermission(permissionShared, "并集公共权限");
            permissionIdA = insertPermission(permissionA, "并集权限A");
            permissionIdB = insertPermission(permissionB, "并集权限B");

            jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)", roleIdA, permissionIdShared);
            jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)", roleIdA, permissionIdA);
            jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)", roleIdB, permissionIdShared);
            jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)", roleIdB, permissionIdB);

            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleIdA);
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleIdB);

            List<String> mapperPermissions = sysUserMapper.selectPermissionCodesByUserId(userId);
            assertThat(mapperPermissions)
                    .containsExactlyInAnyOrder(permissionShared, permissionA, permissionB);

            LoginResponse.UserInfo userInfo = authService.getCurrentUserInfo(userId);
            assertThat(userInfo.getPermissions())
                    .containsExactlyInAnyOrder(permissionShared, permissionA, permissionB);
            assertThat(userInfo.getRoles())
                    .containsExactlyInAnyOrder(roleCodeA, roleCodeB);
        } finally {
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
            }
            if (roleIdA != null || roleIdB != null) {
                jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id IN (?, ?)", roleIdA, roleIdB);
            }
            if (roleIdA != null) {
                jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", roleIdA);
            }
            if (roleIdB != null) {
                jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", roleIdB);
            }
            if (permissionIdShared != null || permissionIdA != null || permissionIdB != null) {
                jdbcTemplate.update(
                        "DELETE FROM sys_permission WHERE id IN (?, ?, ?)",
                        permissionIdShared, permissionIdA, permissionIdB
                );
            }
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
            }
        }
    }

    private Long insertPermission(String permissionCode, String permissionName) {
        jdbcTemplate.update(
                "INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, sort_order, status) VALUES (?, ?, ?, ?, ?, ?)",
                permissionCode,
                permissionName,
                3,
                0,
                0,
                1
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_permission WHERE permission_code = ?",
                Long.class,
                permissionCode
        );
    }
}
