package com.lab.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.annotation.AuditLog;
import com.lab.common.result.Result;
import com.lab.user.dto.UserDTO;
import com.lab.user.entity.SysUser;
import com.lab.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "查询用户列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CENTER_ADMIN')")
    public Result<Page<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType) {
        Page<SysUser> result = userService.listUsers(page, size, keyword, userType);
        return Result.success(result);
    }

    /**
     * 根据ID查询用户
     */
    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CENTER_ADMIN')")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CENTER_ADMIN')")
    @AuditLog(operationType = "CREATE", businessType = "USER", description = "创建用户")
    public Result<Long> createUser(
            @RequestAttribute("userId") Long operatorId,
            @Valid @RequestBody UserDTO userDTO) {
        Long userId = userService.createUser(operatorId, userDTO);
        return Result.success(userId);
    }

    /**
     * 更新用户
     */
    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CENTER_ADMIN')")
    @AuditLog(operationType = "UPDATE", businessType = "USER", description = "更新用户")
    public Result<Void> updateUser(
            @RequestAttribute("userId") Long operatorId,
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {
        userDTO.setId(id);
        userService.updateUser(operatorId, userDTO);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CENTER_ADMIN')")
    @AuditLog(operationType = "DELETE", businessType = "USER", description = "删除用户")
    public Result<Void> deleteUser(
            @RequestAttribute("userId") Long operatorId,
            @PathVariable Long id) {
        userService.deleteUser(operatorId, id);
        return Result.success();
    }

    /**
     * 查询用户角色
     */
    @Operation(summary = "查询用户角色")
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CENTER_ADMIN')")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        List<Long> roleIds = userService.getUserRoleIds(id);
        return Result.success(roleIds);
    }

    /**
     * 分配用户角色
     */
    @Operation(summary = "分配用户角色")
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CENTER_ADMIN')")
    @AuditLog(operationType = "UPDATE", businessType = "USER_ROLE", description = "分配用户角色")
    public Result<Void> assignUserRoles(
            @RequestAttribute("userId") Long operatorId,
            @PathVariable Long id,
            @RequestBody List<Long> roleIds) {
        userService.assignUserRoles(operatorId, id, roleIds);
        return Result.success();
    }
}
