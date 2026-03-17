package com.lab.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色DTO
 */
@Data
public class RoleDTO {
    
    private Long id;
    
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;
    
    @NotBlank(message = "角色名称不能为空")
    private String roleName;
    
    private String description;
    
    private Integer status;
    
    private List<Long> permissionIds;
}
