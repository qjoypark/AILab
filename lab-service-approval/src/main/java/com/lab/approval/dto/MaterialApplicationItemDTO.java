package com.lab.approval.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 领用申请明细DTO
 */
@Data
public class MaterialApplicationItemDTO {
    
    /**
     * 明细ID
     */
    private Long id;
    
    /**
     * 药品ID
     */
    private Long materialId;
    
    /**
     * 药品名称
     */
    private String materialName;
    
    /**
     * 规格
     */
    private String specification;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 申请数量
     */
    private BigDecimal applyQuantity;
    
    /**
     * 批准数量
     */
    private BigDecimal approvedQuantity;
    
    /**
     * 实际出库数量
     */
    private BigDecimal actualQuantity;
    
    /**
     * 备注
     */
    private String remark;
}
