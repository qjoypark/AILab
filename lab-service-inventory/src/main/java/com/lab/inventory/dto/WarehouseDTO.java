package com.lab.inventory.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 仓库DTO
 */
@Data
public class WarehouseDTO {
    
    private Long id;
    
    @NotBlank(message = "仓库编码不能为空")
    private String warehouseCode;
    
    @NotBlank(message = "仓库名称不能为空")
    private String warehouseName;
    
    @NotNull(message = "仓库类型不能为空")
    private Integer warehouseType;
    
    private String location;
    
    private Long managerId;
    
    private Integer status;
}
