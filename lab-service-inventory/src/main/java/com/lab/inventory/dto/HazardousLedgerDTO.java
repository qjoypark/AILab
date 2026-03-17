package com.lab.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 危化品台账报表DTO
 */
@Data
public class HazardousLedgerDTO {
    
    /**
     * 药品ID
     */
    private Long materialId;
    
    /**
     * 药品名称
     */
    private String materialName;
    
    /**
     * CAS号
     */
    private String casNumber;
    
    /**
     * 危险类别
     */
    private String dangerCategory;
    
    /**
     * 管控类型: 0-否, 1-易制毒, 2-易制爆
     */
    private Integer controlType;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 期初库存
     */
    private BigDecimal openingStock;
    
    /**
     * 入库总量
     */
    private BigDecimal totalStockIn;
    
    /**
     * 出库总量
     */
    private BigDecimal totalStockOut;
    
    /**
     * 期末库存
     */
    private BigDecimal closingStock;
    
    /**
     * 账实差异百分比
     */
    private BigDecimal discrepancyRate;
}
