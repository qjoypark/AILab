package com.lab.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预警记录DTO
 */
@Data
public class AlertRecordDTO {
    
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
    
    private String handlerName;
    
    private LocalDateTime handleTime;
    
    private String handleRemark;
}
