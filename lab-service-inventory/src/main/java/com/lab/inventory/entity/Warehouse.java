package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓库实体
 */
@Data
@TableName("warehouse")
public class Warehouse {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String warehouseCode;
    
    private String warehouseName;
    
    private Integer warehouseType;
    
    private String location;
    
    private Long managerId;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    @TableLogic
    private Integer deleted;
}
