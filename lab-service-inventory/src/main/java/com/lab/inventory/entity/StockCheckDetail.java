package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存盘点明细实体
 */
@Data
@TableName("stock_check_detail")
public class StockCheckDetail {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long checkId;
    
    private Long materialId;
    
    private String batchNumber;
    
    private BigDecimal bookQuantity;
    
    private BigDecimal actualQuantity;
    
    private BigDecimal diffQuantity;
    
    private String diffReason;
    
    private Long storageLocationId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
