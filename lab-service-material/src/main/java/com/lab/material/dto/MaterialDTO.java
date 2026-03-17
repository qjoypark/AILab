package com.lab.material.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 药品信息DTO
 */
@Data
public class MaterialDTO {
    
    private Long id;
    
    private String materialCode;
    
    private String materialName;
    
    private Integer materialType;
    
    private Long categoryId;
    
    private String categoryName;
    
    private String specification;
    
    private String unit;
    
    private String casNumber;
    
    private String dangerCategory;
    
    private Integer isControlled;
    
    private Long supplierId;
    
    private String supplierName;
    
    private BigDecimal unitPrice;
    
    private Integer safetyStock;
    
    private Integer maxStock;
    
    private String storageCondition;
    
    private Integer shelfLifeDays;
    
    private String description;
    
    private String imageUrl;
    
    private Integer status;
}
