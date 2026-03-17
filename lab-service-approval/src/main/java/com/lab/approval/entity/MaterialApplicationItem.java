package com.lab.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领用申请明细实体
 */
@Data
@TableName("material_application_item")
public class MaterialApplicationItem {
    
    /**
     * 明细ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 申请单ID
     */
    private Long applicationId;
    
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
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
