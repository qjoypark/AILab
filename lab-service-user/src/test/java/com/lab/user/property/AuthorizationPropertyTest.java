package com.lab.user.property;

import com.lab.user.entity.SysUser;
import com.lab.user.mapper.SysUserMapper;
import com.lab.user.util.JwtUtil;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限控制属性测试
 * 
 * **Validates: Requirements 1.5**
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("Feature: smart-lab-management-system, Property 2: 无权限访问被正确拒绝")
public class AuthorizationPropertyTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 属性 2: 无权限访问被正确拒绝
     * 
     * 对于任何用户和任何需要权限的资源，当用户尝试访问该资源但不具有相应权限时，
     * 系统应拒绝访问并返回权限不足的错误信息。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 2: 无权限访问被正确拒绝")
    void unauthorizedAccessIsRejected(
            @ForAll("usersWithoutAdminRole") TestUser testUser) {
        
        // Given: 创建没有管理员权限的用户
        Long userId = createTestUser(testUser);
        assignNonAdminRole(userId);
        
        // Given: 生成用户令牌
        List<String> roles = getRoleCodesForUser(userId);
        List<String> permissions = getPermissionsForUser(userId);
        String token = jwtUtil.generateAccessToken(userId, testUser.username, roles, permissions);
        
        // 存储令牌到Redis
        redisTemplate.opsForValue().set(
                "token:" + userId,
                token,
                2,
                TimeUnit.HOURS
        );
        
        try {
            // When: 用户尝试访问需要管理员权限的接口（创建用户）
            // Then: 应该返回403权限不足错误
            mockMvc.perform(get("/api/v1/system/users")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403001))
                    .andExpect(jsonPath("$.message").value("无权限访问"));
            
        } catch (Exception e) {
            throw new RuntimeException("测试执行失败", e);
        } finally {
            // Cleanup: 清理测试数据
            cleanupTestUser(userId);
        }
    }
    
    @Provide
    Arbitrary<TestUser> usersWithoutAdminRole() {
        Arbitrary<String> usernames = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(20);
        
        Arbitrary<String> realNames = Arbitraries.strings()
                .alpha()
                .ofMinLength(2)
                .ofMaxLength(10);
        
        // 用户类型：2-教师，3-学生（不包括1-管理员）
        Arbitrary<Integer> userTypes = Arbitraries.integers().between(2, 3);
        
        return Combinators.combine(usernames, realNames, userTypes)
                .as((username, realName, userType) -> {
                    TestUser user = new TestUser();
                    user.username = "test_nonadmin_" + username;
                    user.realName = realName;
                    user.userType = userType;
                    return user;
                });
    }
    
    private Long createTestUser(TestUser testUser) {
        SysUser user = new SysUser();
        user.setUsername(testUser.username);
        user.setPassword(passwordEncoder.encode("test123"));
        user.setRealName(testUser.realName);
        user.setUserType(testUser.userType);
        user.setStatus(1);
        user.setDepartment("测试部门");
        
        userMapper.insert(user);
        return user.getId();
    }
    
    private void assignNonAdminRole(Long userId) {
        // 查找非管理员角色（教师或学生角色）
        List<Long> roleIds = jdbcTemplate.queryForList(
                "SELECT id FROM sys_role WHERE role_code IN ('TEACHER', 'STUDENT') AND status = 1 AND deleted = 0 LIMIT 1",
                Long.class
        );
        
        Long roleId;
        if (roleIds.isEmpty()) {
            // 创建测试角色（如果不存在）
            jdbcTemplate.update(
                    "INSERT INTO sys_role (role_code, role_name, status) VALUES (?, ?, 1)",
                    "STUDENT", "学生"
            );
            roleId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            roleId = roleIds.get(0);
        }
        
        // 分配角色给用户
        jdbcTemplate.update(
                "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                userId, roleId
        );
    }
    
    private List<String> getRoleCodesForUser(Long userId) {
        List<String> roles = jdbcTemplate.queryForList(
                "SELECT r.role_code FROM sys_role r " +
                        "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
                        "WHERE ur.user_id = ? AND r.status = 1 AND r.deleted = 0",
                String.class,
                userId
        );
        return roles != null ? roles : new ArrayList<>();
    }
    
    private List<String> getPermissionsForUser(Long userId) {
        List<String> permissions = jdbcTemplate.queryForList(
                "SELECT DISTINCT p.permission_code FROM sys_permission p " +
                        "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
                        "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
                        "WHERE ur.user_id = ? AND p.status = 1",
                String.class,
                userId
        );
        return permissions != null ? permissions : new ArrayList<>();
    }
    
    private void cleanupTestUser(Long userId) {
        redisTemplate.delete("token:" + userId);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
    }
    
    static class TestUser {
        String username;
        String realName;
        Integer userType;
    }
}
