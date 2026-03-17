package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 存储位置实体
 */
@Data
@TableName("storage_location")
public class StorageLocation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long warehouseId;
    
    private String locationCode;
    
    private String locationName;
    
    private String shelfNumber;
    
    private String layerNumber;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    @TableLogic
    private Integer deleted;
}
