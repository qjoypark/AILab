package com.lab.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.exception.BusinessException;
import com.lab.common.result.ResultCode;
import com.lab.user.dto.LoginRequest;
import com.lab.user.dto.LoginResponse;
import com.lab.user.entity.SysUser;
import com.lab.user.mapper.SysUserMapper;
import com.lab.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    
    private static final String TOKEN_PREFIX = "token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    
    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        
        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(403001, "用户已被禁用");
        }
        
        // 查询用户角色和权限
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionCodesByUserId(user.getId());
        
        // 生成令牌
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        
        // 存储令牌到Redis
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + user.getId(),
                accessToken,
                2,
                TimeUnit.HOURS
        );
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                7,
                TimeUnit.DAYS
        );
        
        // 构建响应
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getUserType())
                .department(user.getDepartment())
                .roles(roles)
                .permissions(permissions)
                .build();
        
        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userInfo(userInfo)
                .build();
    }
    
    /**
     * 刷新令牌
     */
    public LoginResponse refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
        
        // 获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        
        // 验证Redis中的刷新令牌
        String storedRefreshToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
        
        // 查询用户信息
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 查询用户角色和权限
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionCodesByUserId(user.getId());
        
        // 生成新的访问令牌
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        
        // 更新Redis中的令牌
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + user.getId(),
                newAccessToken,
                2,
                TimeUnit.HOURS
        );
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                newRefreshToken,
                7,
                TimeUnit.DAYS
        );
        
        // 构建响应
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getUserType())
                .department(user.getDepartment())
                .roles(roles)
                .permissions(permissions)
                .build();
        
        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userInfo(userInfo)
                .build();
    }
    
    /**
     * 用户登出
     */
    public void logout(Long userId) {
        // 删除Redis中的令牌
        redisTemplate.delete(TOKEN_PREFIX + userId);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}
