package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存实体
 */
@Data
@TableName("stock_inventory")
public class StockInventory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long materialId;
    
    private Long warehouseId;
    
    private Long storageLocationId;
    
    private String batchNumber;
    
    private BigDecimal quantity;
    
    private BigDecimal availableQuantity;
    
    private BigDecimal lockedQuantity;
    
    private LocalDate productionDate;
    
    private LocalDate expireDate;
    
    private BigDecimal unitPrice;
    
    private BigDecimal totalAmount;
    
    private LocalDate lastCheckDate;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
