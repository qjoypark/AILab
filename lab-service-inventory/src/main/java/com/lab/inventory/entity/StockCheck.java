package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存盘点实体
 */
@Data
@TableName("stock_check")
public class StockCheck {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String checkNo;
    
    private Long warehouseId;
    
    private LocalDate checkDate;
    
    private Long checkerId;
    
    private Integer status;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    @TableLogic
    private Integer deleted;
}
