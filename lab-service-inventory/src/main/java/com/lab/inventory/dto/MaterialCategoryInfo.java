package com.lab.inventory.dto;

import lombok.Data;

/**
 * 药品分类信息DTO
 */
@Data
public class MaterialCategoryInfo {
    
    /**
     * 分类ID
     */
    private Long id;
    
    /**
     * 分类编码
     */
    private String categoryCode;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 父分类ID
     */
    private Long parentId;
    
    /**
     * 分类层级
     */
    private Integer categoryLevel;
}
