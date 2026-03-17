package com.lab.material.dto;

import lombok.Data;

import java.util.List;

/**
 * 药品分类DTO
 */
@Data
public class MaterialCategoryDTO {
    
    private Long id;
    
    private String categoryCode;
    
    private String categoryName;
    
    private Long parentId;
    
    private Integer categoryLevel;
    
    private Integer sortOrder;
    
    private String description;
    
    private List<MaterialCategoryDTO> children;
}
