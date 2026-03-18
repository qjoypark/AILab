package com.lab.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.exception.BusinessException;
import com.lab.user.dto.RoleDTO;
import com.lab.user.entity.SysPermission;
import com.lab.user.entity.SysRole;
import com.lab.user.mapper.SysPermissionMapper;
import com.lab.user.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 查询所有角色
     */
    public List<SysRole> listRoles() {
        return roleMapper.selectList(null);
    }

    /**
     * 根据ID查询角色
     */
    public SysRole getRoleById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404001, "角色不存在");
        }
        return role;
    }

    /**
     * 创建角色
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleDTO roleDTO) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, roleDTO.getRoleCode());
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400001, "角色编码已存在");
        }

        SysRole role = new SysRole();
        BeanUtils.copyProperties(roleDTO, role);
        roleMapper.insert(role);

        if (roleDTO.getPermissionIds() != null && !roleDTO.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), roleDTO.getPermissionIds());
        }

        return role.getId();
    }

    /**
     * 更新角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleDTO roleDTO) {
        SysRole existingRole = getRoleById(roleDTO.getId());

        if (!existingRole.getRoleCode().equals(roleDTO.getRoleCode())) {
            LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysRole::getRoleCode, roleDTO.getRoleCode());
            wrapper.ne(SysRole::getId, roleDTO.getId());
            if (roleMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(400001, "角色编码已存在");
            }
        }

        SysRole role = new SysRole();
        BeanUtils.copyProperties(roleDTO, role);
        roleMapper.updateById(role);

        if (roleDTO.getPermissionIds() != null) {
            jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleDTO.getId());
            if (!roleDTO.getPermissionIds().isEmpty()) {
                assignPermissions(roleDTO.getId(), roleDTO.getPermissionIds());
            }
        }
    }

    /**
     * 删除角色（逻辑删除）
     */
    public void deleteRole(Long id) {
        getRoleById(id);
        roleMapper.deleteById(id);
    }

    /**
     * 分配角色权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);

        for (Long permissionId : permissionIds) {
            jdbcTemplate.update(
                    "INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)",
                    roleId, permissionId
            );
        }
    }

    /**
     * 查询角色的权限ID列表
     */
    public List<Long> getRolePermissionIds(Long roleId) {
        return jdbcTemplate.queryForList(
                "SELECT permission_id FROM sys_role_permission WHERE role_id = ?",
                Long.class,
                roleId
        );
    }

    /**
     * 查询权限树
     */
    public List<Map<String, Object>> getPermissionTree() {
        List<SysPermission> allPermissions = permissionMapper.selectList(null);
        return buildPermissionTree(allPermissions, 0L);
    }

    private List<Map<String, Object>> buildPermissionTree(List<SysPermission> allPermissions, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();

        List<SysPermission> children = allPermissions.stream()
                .filter(permission -> Objects.equals(permission.getParentId(), parentId))
                .sorted(Comparator.comparing(SysPermission::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        for (SysPermission permission : children) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", permission.getId());
            node.put("permissionCode", permission.getPermissionCode());
            node.put("permissionName", permission.getPermissionName());
            node.put("permissionType", permission.getPermissionType());
            node.put("path", permission.getPath());
            node.put("icon", permission.getIcon());
            node.put("sortOrder", permission.getSortOrder());
            node.put("status", permission.getStatus());

            List<Map<String, Object>> childNodes = buildPermissionTree(allPermissions, permission.getId());
            if (!childNodes.isEmpty()) {
                node.put("children", childNodes);
            }

            tree.add(node);
        }

        return tree;
    }
}
