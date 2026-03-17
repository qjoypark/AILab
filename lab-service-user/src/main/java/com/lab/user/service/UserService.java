package com.lab.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.common.result.ResultCode;
import com.lab.user.dto.UserDTO;
import com.lab.user.entity.SysUser;
import com.lab.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
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
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword));
        }
        if (userType != null) {
            wrapper.eq(SysUser::getUserType, userType);
        }
        
        return userMapper.selectPage(pageParam, wrapper);
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
    public Long createUser(UserDTO userDTO) {
        // 检查用户名是否存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, userDTO.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        
        // 创建用户
        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);
        
        // 加密密码
        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        } else {
            // 默认密码
            user.setPassword(passwordEncoder.encode("123456"));
        }
        
        userMapper.insert(user);
        
        // 分配角色
        if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), userDTO.getRoleIds());
        }
        
        return user.getId();
    }
    
    /**
     * 更新用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserDTO userDTO) {
        SysUser existingUser = getUserById(userDTO.getId());
        
        // 检查用户名是否被其他用户使用
        if (!existingUser.getUsername().equals(userDTO.getUsername())) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, userDTO.getUsername());
            wrapper.ne(SysUser::getId, userDTO.getId());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ResultCode.USERNAME_EXISTS);
            }
        }
        
        // 更新用户信息
        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);
        
        // 如果提供了新密码，则加密
        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码
        }
        
        userMapper.updateById(user);
        
        // 更新角色
        if (userDTO.getRoleIds() != null) {
            // 删除旧角色
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userDTO.getId());
            // 分配新角色
            if (!userDTO.getRoleIds().isEmpty()) {
                assignRoles(userDTO.getId(), userDTO.getRoleIds());
            }
        }
    }
    
    /**
     * 删除用户（逻辑删除）
     */
    public void deleteUser(Long id) {
        getUserById(id);
        userMapper.deleteById(id);
    }

    /**
     * 鍗曠嫭鍒嗛厤鐢ㄦ埛瑙掕壊
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        getUserById(userId);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            assignRoles(userId, roleIds);
        }
    }
    
    /**
     * 分配角色
     */
    private void assignRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            jdbcTemplate.update(
                    "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                    userId, roleId
            );
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
}
