package com.lab.user.security;

import com.lab.user.entity.SysUser;
import com.lab.user.mapper.SysUserMapper;
import com.lab.user.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper userMapper;
    
    private static final String ACCESS_TOKEN_PREFIX = "access_token:";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 获取令牌
        String token = getTokenFromRequest(request);
        
        if (StringUtils.hasText(token)) {
            try {
                // 验证令牌
                if (jwtUtil.validateToken(token)) {
                    // 解析令牌
                    Claims claims = jwtUtil.parseToken(token);
                    Long userId = Long.parseLong(claims.getSubject());
                    
                    // 验证Redis中的令牌（按token维度，支持多会话并存）
                    String storedUserId = redisTemplate.opsForValue().get(ACCESS_TOKEN_PREFIX + token);
                    if (storedUserId != null && storedUserId.equals(String.valueOf(userId))) {
                        SysUser user = userMapper.selectById(userId);
                        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
                            log.warn("用户状态无效，忽略当前令牌认证: userId={}", userId);
                            filterChain.doFilter(request, response);
                            return;
                        }

                        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
                        List<String> permissions = userMapper.selectPermissionCodesByUserId(userId);
                        
                        // 构建权限列表
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        if (roles != null) {
                            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                        }
                        if (permissions != null) {
                            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
                        }
                        
                        // 创建认证对象
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
                        
                        // 设置到Security上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // 设置用户ID到请求属性
                        request.setAttribute("userId", userId);
                    }
                }
            } catch (Exception e) {
                log.error("JWT认证失败", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求中获取令牌
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
