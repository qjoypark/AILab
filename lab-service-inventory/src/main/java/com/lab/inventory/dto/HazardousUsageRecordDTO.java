package com.lab.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 危化品使用记录DTO
 */
@Data
public class HazardousUsageRecordDTO {
    
    /**
     * 申请单ID
     */
    private Long applicationId;
    
    /**
     * 申请单号
     */
    private String applicationNo;
    
    /**
     * 药品ID
     */
    private Long materialId;
    
    /**
     * 药品名称
     */
    private String materialName;
    
    /**
     * 使用人ID
     */
    private Long userId;
    
    /**
     * 使用人姓名
     */
    private String userName;
    
    /**
     * 领用数量
     */
    private BigDecimal receivedQuantity;
    
    /**
     * 使用日期
     */
    private LocalDate usageDate;
    
    /**
     * 使用地点
     */
    private String usageLocation;
    
    /**
     * 使用目的
     */
    private String usagePurpose;
}
