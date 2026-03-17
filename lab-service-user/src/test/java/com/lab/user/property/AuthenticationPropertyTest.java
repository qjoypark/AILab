package com.lab.user.property;

import com.lab.user.dto.LoginRequest;
import com.lab.user.dto.LoginResponse;
import com.lab.user.entity.SysUser;
import com.lab.user.mapper.SysUserMapper;
import com.lab.user.service.AuthService;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户认证属性测试
 * 
 * **Validates: Requirements 1.2**
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("Feature: smart-lab-management-system, Property 1: 用户登录后角色权限正确分配")
public class AuthenticationPropertyTest {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 属性 1: 用户登录后角色权限正确分配
     * 
     * 对于任何用户，当该用户成功登录系统时，系统应返回该用户的所有角色，
     * 并且每个角色应包含其对应的权限列表。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 1: 用户登录后角色权限正确分配")
    void userLoginAssignsRolesAndPermissionsCorrectly(
            @ForAll("validUsers") TestUser testUser) {
        
        // Given: 创建测试用户和角色权限
        Long userId = createTestUser(testUser);
        List<Long> assignedRoleIds = assignRolesToUser(userId, testUser.roleCount);
        List<String> expectedRoleCodes = getRoleCodesForUser(userId);
        List<String> expectedPermissions = getPermissionsForUser(userId);
        
        try {
            // When: 用户登录
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(testUser.username);
            loginRequest.setPassword(testUser.password);
            
            LoginResponse response = authService.login(loginRequest);
            
            // Then: 验证返回的用户信息包含所有角色
            assertThat(response).isNotNull();
            assertThat(response.getUserInfo()).isNotNull();
            assertThat(response.getUserInfo().getRoles())
                    .as("用户应该拥有所有分配的角色")
                    .isNotNull()
                    .containsExactlyInAnyOrderElementsOf(expectedRoleCodes);
            
            // Then: 验证返回的用户信息包含所有权限
            assertThat(response.getUserInfo().getPermissions())
                    .as("用户应该拥有所有角色对应的权限")
                    .isNotNull()
                    .containsExactlyInAnyOrderElementsOf(expectedPermissions);
            
            // Then: 验证令牌不为空
            assertThat(response.getToken())
                    .as("访问令牌不应为空")
                    .isNotBlank();
            assertThat(response.getRefreshToken())
                    .as("刷新令牌不应为空")
                    .isNotBlank();
            
        } finally {
            // Cleanup: 清理测试数据
            cleanupTestUser(userId);
        }
    }
    
    @Provide
    Arbitrary<TestUser> validUsers() {
        Arbitrary<String> usernames = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(20);
        
        Arbitrary<String> passwords = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(6)
                .ofMaxLength(20);
        
        Arbitrary<String> realNames = Arbitraries.strings()
                .alpha()
                .ofMinLength(2)
                .ofMaxLength(10);
        
        Arbitrary<Integer> userTypes = Arbitraries.integers().between(1, 3);
        
        Arbitrary<Integer> roleCounts = Arbitraries.integers().between(1, 3);
        
        return Combinators.combine(usernames, passwords, realNames, userTypes, roleCounts)
                .as((username, password, realName, userType, roleCount) -> {
                    TestUser user = new TestUser();
                    user.username = "test_" + username;
                    user.password = password;
                    user.realName = realName;
                    user.userType = userType;
                    user.roleCount = roleCount;
                    return user;
                });
    }
    
    private Long createTestUser(TestUser testUser) {
        SysUser user = new SysUser();
        user.setUsername(testUser.username);
        user.setPassword(passwordEncoder.encode(testUser.password));
        user.setRealName(testUser.realName);
        user.setUserType(testUser.userType);
        user.setStatus(1);
        user.setDepartment("测试部门");
        
        userMapper.insert(user);
        return user.getId();
    }
    
    private List<Long> assignRolesToUser(Long userId, int roleCount) {
        // 获取系统中已存在的角色
        List<Long> availableRoleIds = jdbcTemplate.queryForList(
                "SELECT id FROM sys_role WHERE status = 1 AND deleted = 0 LIMIT ?",
                Long.class,
                roleCount
        );
        
        // 如果没有足够的角色，创建测试角色
        while (availableRoleIds.size() < roleCount) {
            String roleCode = "TEST_ROLE_" + System.currentTimeMillis() + "_" + availableRoleIds.size();
            jdbcTemplate.update(
                    "INSERT INTO sys_role (role_code, role_name, status) VALUES (?, ?, 1)",
                    roleCode, "测试角色" + availableRoleIds.size()
            );
            Long roleId = jdbcTemplate.queryForObject(
                    "SELECT LAST_INSERT_ID()",
                    Long.class
            );
            availableRoleIds.add(roleId);
        }
        
        // 分配角色给用户
        for (Long roleId : availableRoleIds) {
            jdbcTemplate.update(
                    "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                    userId, roleId
            );
        }
        
        return availableRoleIds;
    }
    
    private List<String> getRoleCodesForUser(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT r.role_code FROM sys_role r " +
                        "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
                        "WHERE ur.user_id = ? AND r.status = 1 AND r.deleted = 0",
                String.class,
                userId
        );
    }
    
    private List<String> getPermissionsForUser(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT p.permission_code FROM sys_permission p " +
                        "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
                        "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
                        "WHERE ur.user_id = ? AND p.status = 1",
                String.class,
                userId
        );
    }
    
    private void cleanupTestUser(Long userId) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
    }
    
    static class TestUser {
        String username;
        String password;
        String realName;
        Integer userType;
        Integer roleCount;
    }
}
