package com.lab.material.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 药品信息实体
 */
@Data
@TableName("material")
public class Material {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String materialCode;
    
    private String materialName;
    
    private Integer materialType;
    
    private Long categoryId;
    
    private String specification;
    
    private String unit;
    
    private String casNumber;
    
    private String dangerCategory;
    
    private Integer isControlled;
    
    private Long supplierId;
    
    private BigDecimal unitPrice;
    
    private Integer safetyStock;
    
    private Integer maxStock;
    
    private String storageCondition;
    
    private Integer shelfLifeDays;
    
    private String description;
    
    private String imageUrl;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    @TableLogic
    private Integer deleted;
}
