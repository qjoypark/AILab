package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入库单明细实体
 */
@Data
@TableName("stock_in_detail")
public class StockInDetail {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long inOrderId;
    
    private Long materialId;
    
    private String batchNumber;
    
    private BigDecimal quantity;
    
    private BigDecimal unitPrice;
    
    private BigDecimal totalAmount;
    
    private LocalDate productionDate;
    
    private LocalDate expireDate;
    
    private Long storageLocationId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
