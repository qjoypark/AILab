package com.lab.material.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 药品分类实体
 */
@Data
@TableName("material_category")
public class MaterialCategory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String categoryCode;
    
    private String categoryName;
    
    private Long parentId;
    
    private Integer categoryLevel;
    
    private Integer sortOrder;
    
    private String description;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    @TableLogic
    private Integer deleted;
}
