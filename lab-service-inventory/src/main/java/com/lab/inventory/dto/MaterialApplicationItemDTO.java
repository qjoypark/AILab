package com.lab.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 领用申请明细DTO（简化版，用于库存服务）
 */
@Data
public class MaterialApplicationItemDTO {
    
    private Long id;
    
    private Long materialId;
    
    private String materialName;
    
    private String specification;
    
    private String unit;
    
    private BigDecimal applyQuantity;
    
    private BigDecimal approvedQuantity;
    
    private BigDecimal actualQuantity;
    
    private String remark;
}
