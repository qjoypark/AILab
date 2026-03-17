package com.lab.user.controller;

import com.lab.common.annotation.AuditLog;
import com.lab.common.result.Result;
import com.lab.user.dto.RoleDTO;
import com.lab.user.entity.SysRole;
import com.lab.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
public class RoleController {
    
    private final RoleService roleService;
    
    /**
     * 查询所有角色
     */
    @Operation(summary = "查询角色列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysRole>> listRoles() {
        List<SysRole> roles = roleService.listRoles();
        return Result.success(roles);
    }
    
    /**
     * 根据ID查询角色
     */
    @Operation(summary = "查询角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysRole> getRoleById(@PathVariable Long id) {
        SysRole role = roleService.getRoleById(id);
        return Result.success(role);
    }
    
    /**
     * 创建角色
     */
    @Operation(summary = "创建角色")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(operationType = "CREATE", businessType = "ROLE", description = "创建角色")
    public Result<Long> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        Long roleId = roleService.createRole(roleDTO);
        return Result.success(roleId);
    }
    
    /**
     * 更新角色
     */
    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO roleDTO) {
        roleDTO.setId(id);
        roleService.updateRole(roleDTO);
        return Result.success();
    }
    
    /**
     * 删除角色
     */
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }
    
    /**
     * 查询权限树
     */
    @Operation(summary = "查询权限树")
    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getPermissionTree() {
        List<Map<String, Object>> tree = roleService.getPermissionTree();
        return Result.success(tree);
    }
    
    /**
     * 分配角色权限
     */
    @Operation(summary = "分配角色权限")
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(operationType = "UPDATE", businessType = "ROLE_PERMISSION", description = "分配角色权限")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return Result.success();
    }
    
    /**
     * 查询角色的权限
     */
    @Operation(summary = "查询角色的权限")
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<Long> permissionIds = roleService.getRolePermissionIds(id);
        return Result.success(permissionIds);
    }
}
