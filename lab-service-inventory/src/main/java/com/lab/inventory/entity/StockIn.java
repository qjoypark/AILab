package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入库单实体
 */
@Data
@TableName("stock_in")
public class StockIn {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String inOrderNo;
    
    private Integer inType;
    
    private Long warehouseId;
    
    private Long supplierId;
    
    private BigDecimal totalAmount;
    
    private LocalDate inDate;
    
    private Long operatorId;
    
    private Integer status;
    
    private String remark;
    
    private String attachmentUrl;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private List<StockInDetail> items;

    @TableField(exist = false)
    private String createdByName;
}
