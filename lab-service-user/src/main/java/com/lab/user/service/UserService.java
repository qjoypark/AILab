package com.lab.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.common.result.ResultCode;
import com.lab.user.dto.UserDTO;
import com.lab.user.dto.UserOptionDTO;
import com.lab.user.entity.SysUser;
import com.lab.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CENTER_ADMIN = "CENTER_ADMIN";
    private static final Set<String> CENTER_ADMIN_PROTECTED_TARGET_ROLES = Set.of(
            "ADMIN",
            "COLLEGE_DEAN",
            "DEAN",
            "VICE_DEAN",
            "COLLEGE_VICE_DEAN"
    );

    private final SysUserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    public Page<SysUser> listUsers(int page, int size, String keyword, Integer userType) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword));
        }
        if (userType != null) {
            wrapper.eq(SysUser::getUserType, userType);
        }

        return userMapper.selectPage(pageParam, wrapper);
    }

    public Page<UserOptionDTO> listSelectableUsers(int page,
                                                  int size,
                                                  String keyword,
                                                  Integer userType,
                                                  Integer status) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getDepartment, keyword));
        }
        if (userType != null) {
            wrapper.eq(SysUser::getUserType, userType);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByAsc(SysUser::getRealName).orderByAsc(SysUser::getUsername);

        Page<SysUser> userPage = userMapper.selectPage(pageParam, wrapper);
        Page<UserOptionDTO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(userPage.getRecords().stream()
                .map(UserOptionDTO::fromEntity)
                .toList());
        return result;
    }

    /**
     * 根据ID查询用户
     */
    public SysUser getUserById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 创建用户
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(Long operatorId, UserDTO userDTO) {
        assertOperatorCanManageSystem(operatorId);
        validateCenterAdminAssignableRoles(operatorId, userDTO.getRoleIds(), "创建用户并分配角色");

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, userDTO.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);

        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        } else {
            user.setPassword(passwordEncoder.encode("123456"));
        }

        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), userDTO.getRoleIds());
        }

        return user.getId();
    }

    /**
     * 更新用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long operatorId, UserDTO userDTO) {
        assertOperatorCanManageTarget(operatorId, userDTO.getId(), "编辑");
        validateCenterAdminAssignableRoles(operatorId, userDTO.getRoleIds(), "编辑用户并分配角色");

        SysUser existingUser = getUserById(userDTO.getId());

        if (!existingUser.getUsername().equals(userDTO.getUsername())) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, userDTO.getUsername());
            wrapper.ne(SysUser::getId, userDTO.getId());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ResultCode.USERNAME_EXISTS);
            }
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);

        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        } else {
            user.setPassword(null);
        }

        try {
            userMapper.updateById(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        if (userDTO.getRoleIds() != null) {
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userDTO.getId());
            if (!userDTO.getRoleIds().isEmpty()) {
                assignRoles(userDTO.getId(), userDTO.getRoleIds());
            }
        }
    }

    /**
     * 删除用户（逻辑删除）
     */
    public void deleteUser(Long operatorId, Long targetUserId) {
        assertOperatorCanManageTarget(operatorId, targetUserId, "删除");
        getUserById(targetUserId);
        SysUser recycledUser = new SysUser();
        recycledUser.setId(targetUserId);
        recycledUser.setUsername(buildDeletedUsername(targetUserId));
        userMapper.updateById(recycledUser);
        userMapper.deleteById(targetUserId);
    }

    /**
     * 分配用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long operatorId, Long targetUserId, List<Long> roleIds) {
        assertOperatorCanManageTarget(operatorId, targetUserId, "分配角色");
        validateCenterAdminAssignableRoles(operatorId, roleIds, "分配角色");

        getUserById(targetUserId);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", targetUserId);
        if (roleIds != null && !roleIds.isEmpty()) {
            assignRoles(targetUserId, roleIds);
        }
    }

    /**
     * 查询用户的角色ID列表
     */
    public List<Long> getUserRoleIds(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT role_id FROM sys_user_role WHERE user_id = ?",
                Long.class,
                userId
        );
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            jdbcTemplate.update(
                    "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                    userId, roleId
            );
        }
    }

    private void assertOperatorCanManageTarget(Long operatorId, Long targetUserId, String action) {
        List<String> operatorRoleCodes = userMapper.selectRoleCodesByUserId(operatorId);
        if (containsRole(operatorRoleCodes, ROLE_ADMIN)) {
            return;
        }

        if (!containsRole(operatorRoleCodes, ROLE_CENTER_ADMIN)) {
            throw new BusinessException(403001, "当前用户无系统管理权限");
        }

        if (operatorId.equals(targetUserId)) {
            throw new BusinessException(403001, "实验中心主任不能" + action + "自己");
        }

        List<String> targetRoleCodes = userMapper.selectRoleCodesByUserId(targetUserId);
        boolean targetProtected = targetRoleCodes.stream()
                .map(roleCode -> roleCode == null ? "" : roleCode.toUpperCase(Locale.ROOT))
                .anyMatch(CENTER_ADMIN_PROTECTED_TARGET_ROLES::contains);

        if (targetProtected) {
            throw new BusinessException(403001, "实验中心主任不能" + action + "ADMIN/学院院长/副院长");
        }
    }

    private void assertOperatorCanManageSystem(Long operatorId) {
        List<String> operatorRoleCodes = userMapper.selectRoleCodesByUserId(operatorId);
        if (containsRole(operatorRoleCodes, ROLE_ADMIN) || containsRole(operatorRoleCodes, ROLE_CENTER_ADMIN)) {
            return;
        }
        throw new BusinessException(403001, "当前用户无系统管理权限");
    }

    private void validateCenterAdminAssignableRoles(Long operatorId, List<Long> roleIds, String action) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        List<String> operatorRoleCodes = userMapper.selectRoleCodesByUserId(operatorId);
        if (!containsRole(operatorRoleCodes, ROLE_CENTER_ADMIN) || containsRole(operatorRoleCodes, ROLE_ADMIN)) {
            return;
        }

        for (Long roleId : roleIds) {
            String roleCode = jdbcTemplate.query(
                    "SELECT role_code FROM sys_role WHERE id = ? AND deleted = 0 LIMIT 1",
                    rs -> rs.next() ? rs.getString("role_code") : null,
                    roleId
            );
            if (roleCode == null) {
                continue;
            }
            if (CENTER_ADMIN_PROTECTED_TARGET_ROLES.contains(roleCode.toUpperCase(Locale.ROOT))) {
                throw new BusinessException(403001, "实验中心主任不能" + action + "为ADMIN/学院院长/副院长角色");
            }
        }
    }

    private boolean containsRole(List<String> roleCodes, String expectedRoleCode) {
        return roleCodes.stream()
                .anyMatch(roleCode -> expectedRoleCode.equalsIgnoreCase(roleCode));
    }

    private String buildDeletedUsername(Long userId) {
        return "deleted_" + userId + "_" + System.currentTimeMillis();
    }
}
