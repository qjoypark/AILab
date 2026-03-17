package com.lab.inventory.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 存储位置DTO
 */
@Data
public class StorageLocationDTO {
    
    private Long id;
    
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    
    @NotBlank(message = "位置编码不能为空")
    private String locationCode;
    
    @NotBlank(message = "位置名称不能为空")
    private String locationName;
    
    private String shelfNumber;
    
    private String layerNumber;
    
    private Integer status;
}
