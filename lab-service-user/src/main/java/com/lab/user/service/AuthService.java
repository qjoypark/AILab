package com.lab.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.exception.BusinessException;
import com.lab.common.result.ResultCode;
import com.lab.user.dto.LoginRequest;
import com.lab.user.dto.LoginResponse;
import com.lab.user.dto.UpdateProfileRequest;
import com.lab.user.entity.SysUser;
import com.lab.user.mapper.SysUserMapper;
import com.lab.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    private static final String ACCESS_TOKEN_PREFIX = "access_token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String ACCESS_TO_REFRESH_PREFIX = "access_to_refresh:";

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(403001, "用户已被禁用");
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionCodesByUserId(user.getId());

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        redisTemplate.opsForValue().set(
                ACCESS_TOKEN_PREFIX + accessToken,
                String.valueOf(user.getId()),
                2,
                TimeUnit.HOURS
        );
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshToken,
                accessToken,
                7,
                TimeUnit.DAYS
        );
        redisTemplate.opsForValue().set(
                ACCESS_TO_REFRESH_PREFIX + accessToken,
                refreshToken,
                7,
                TimeUnit.DAYS
        );

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userInfo(buildUserInfo(user, roles, permissions))
                .build();
    }

    /**
     * 刷新令牌
     */
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String storedAccessToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + refreshToken);
        if (storedAccessToken == null) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionCodesByUserId(user.getId());

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        redisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken);
        redisTemplate.delete(ACCESS_TO_REFRESH_PREFIX + storedAccessToken);

        redisTemplate.opsForValue().set(
                ACCESS_TOKEN_PREFIX + newAccessToken,
                String.valueOf(user.getId()),
                2,
                TimeUnit.HOURS
        );
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + newRefreshToken,
                newAccessToken,
                7,
                TimeUnit.DAYS
        );
        redisTemplate.opsForValue().set(
                ACCESS_TO_REFRESH_PREFIX + newAccessToken,
                newRefreshToken,
                7,
                TimeUnit.DAYS
        );

        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userInfo(buildUserInfo(user, roles, permissions))
                .build();
    }

    /**
     * 用户登出
     */
    public void logout(Long userId, String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            String refreshToken = redisTemplate.opsForValue().get(ACCESS_TO_REFRESH_PREFIX + accessToken);
            redisTemplate.delete(ACCESS_TOKEN_PREFIX + accessToken);
            redisTemplate.delete(ACCESS_TO_REFRESH_PREFIX + accessToken);
            if (refreshToken != null && !refreshToken.isBlank()) {
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken);
            }
        }
    }

    /**
     * 查询当前登录用户信息
     */
    public LoginResponse.UserInfo getCurrentUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        List<String> permissions = userMapper.selectPermissionCodesByUserId(userId);
        return buildUserInfo(user, roles, permissions);
    }

    /**
     * 仅允许修改自己的用户名和密码
     */
    public LoginResponse.UserInfo updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser currentUser = userMapper.selectById(userId);
        if (currentUser == null || currentUser.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        String targetUsername = request.getUsername().trim();
        boolean usernameChanged = !targetUsername.equals(currentUser.getUsername());
        boolean passwordChanged = StringUtils.hasText(request.getNewPassword());

        if (!usernameChanged && !passwordChanged) {
            return getCurrentUserInfo(userId);
        }

        if (!StringUtils.hasText(request.getCurrentPassword())
                || !passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        if (usernameChanged) {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, targetUsername);
            wrapper.ne(SysUser::getId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ResultCode.USERNAME_EXISTS);
            }
        }

        SysUser updateUser = new SysUser();
        updateUser.setId(userId);
        if (usernameChanged) {
            updateUser.setUsername(targetUsername);
        }
        if (passwordChanged) {
            updateUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        userMapper.updateById(updateUser);

        return getCurrentUserInfo(userId);
    }

    private LoginResponse.UserInfo buildUserInfo(SysUser user, List<String> roles, List<String> permissions) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getUserType())
                .department(user.getDepartment())
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
