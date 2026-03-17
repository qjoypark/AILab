package com.lab.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:lab-management-system-secret-key-for-jwt-token-generation-2024}")
    private String secret;
    
    @Value("${jwt.access-token-expiration:7200000}") // 2小时
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration:604800000}") // 7天
    private Long refreshTokenExpiration;
    
    /**
     * 生成访问令牌
     */
    public String generateAccessToken(Long userId, String username, List<String> roles, List<String> permissions) {
        return generateToken(userId, username, roles, permissions, accessTokenExpiration);
    }
    
    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, null, null, refreshTokenExpiration);
    }
    
    /**
     * 生成令牌
     */
    private String generateToken(Long userId, String username, List<String> roles, List<String> permissions, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key);
        
        if (roles != null) {
            builder.claim("roles", roles);
        }
        if (permissions != null) {
            builder.claim("permissions", permissions);
        }
        
        return builder.compact();
    }
    
    /**
     * 解析令牌
     */
    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
    
    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }
    
    /**
     * 验证令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
