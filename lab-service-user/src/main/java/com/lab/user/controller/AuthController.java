package com.lab.user.controller;

import com.lab.common.result.Result;
import com.lab.user.dto.LoginRequest;
import com.lab.user.dto.LoginResponse;
import com.lab.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }
    
    /**
     * 刷新令牌
     */
    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestHeader("Authorization") String authorization) {
        String refreshToken = authorization.replace("Bearer ", "");
        LoginResponse response = authService.refreshToken(refreshToken);
        return Result.success(response);
    }
    
    /**
     * 用户登出
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute("userId") Long userId) {
        authService.logout(userId);
        return Result.success();
    }
}
