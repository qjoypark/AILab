package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存预警配置实体
 */
@Data
@TableName("stock_alert_config")
public class StockAlertConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long materialId;
    
    private Integer alertType;
    
    private BigDecimal thresholdValue;
    
    private Integer alertDays;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
