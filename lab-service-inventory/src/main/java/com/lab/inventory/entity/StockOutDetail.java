package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出库单明细实体
 */
@Data
@TableName("stock_out_detail")
public class StockOutDetail {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long outOrderId;
    
    private Long materialId;
    
    private String batchNumber;
    
    private BigDecimal quantity;
    
    private BigDecimal unitPrice;
    
    private BigDecimal totalAmount;
    
    private Long storageLocationId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
