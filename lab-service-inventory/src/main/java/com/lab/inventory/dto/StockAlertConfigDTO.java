package com.lab.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存预警配置DTO
 */
@Data
public class StockAlertConfigDTO {
    
    private Long id;
    
    private Long materialId;
    
    private String materialName;
    
    private Integer alertType;
    
    private BigDecimal thresholdValue;
    
    private Integer alertDays;
    
    private Integer status;
}
