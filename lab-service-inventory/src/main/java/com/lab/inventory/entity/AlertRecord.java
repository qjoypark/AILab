package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预警记录实体
 */
@Data
@TableName("alert_record")
public class AlertRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Integer alertType;
    
    private Integer alertLevel;
    
    private String businessType;
    
    private Long businessId;
    
    private String alertTitle;
    
    private String alertContent;
    
    private LocalDateTime alertTime;
    
    private Integer status;
    
    private Long handlerId;
    
    private LocalDateTime handleTime;
    
    private String handleRemark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
