package com.lab.inventory.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 领用申请单DTO（简化版，用于库存服务）
 */
@Data
public class MaterialApplicationDTO {
    
    private Long id;
    
    private String applicationNo;
    
    private Long applicantId;
    
    private String applicantName;
    
    private String applicantDept;
    
    private Integer applicationType;
    
    private String usagePurpose;
    
    private String usageLocation;
    
    private LocalDate expectedDate;
    
    private Integer status;
    
    private Integer approvalStatus;
    
    private LocalDateTime createdTime;
    
    private List<MaterialApplicationItemDTO> items;
}
