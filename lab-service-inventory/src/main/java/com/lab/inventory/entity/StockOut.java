package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出库单实体
 */
@Data
@TableName("stock_out")
public class StockOut {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String outOrderNo;
    
    private Integer outType;
    
    private Long warehouseId;
    
    private Long applicationId;
    
    private Long receiverId;
    
    private String receiverName;
    
    private String receiverDept;
    
    private LocalDate outDate;
    
    private Long operatorId;
    
    private Integer status;
    
    private String remark;
    
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
    private String applicationNo;

    @TableField(exist = false)
    private List<StockOutDetail> items;
}
