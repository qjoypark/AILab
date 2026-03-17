package com.lab.inventory.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 危化品台账查询参数DTO
 */
@Data
public class HazardousLedgerQueryDTO {
    
    /**
     * 开始日期
     */
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    private LocalDate endDate;
    
    /**
     * 药品ID（可选，用于筛选特定药品）
     */
    private Long materialId;
}
