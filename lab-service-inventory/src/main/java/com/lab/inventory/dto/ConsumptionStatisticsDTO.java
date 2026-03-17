package com.lab.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 消耗统计报表DTO
 */
@Data
@Schema(description = "消耗统计报表")
public class ConsumptionStatisticsDTO {
    
    @Schema(description = "总消耗数量")
    private BigDecimal totalConsumption;
    
    @Schema(description = "总消耗成本")
    private BigDecimal totalCost;
    
    @Schema(description = "物料消耗明细列表")
    private List<MaterialConsumption> materials;
    
    /**
     * 物料消耗明细
     */
    @Data
    @Schema(description = "物料消耗明细")
    public static class MaterialConsumption {
        
        @Schema(description = "物料ID")
        private Long materialId;
        
        @Schema(description = "物料名称")
        private String materialName;
        
        @Schema(description = "物料编码")
        private String materialCode;
        
        @Schema(description = "规格")
        private String specification;
        
        @Schema(description = "单位")
        private String unit;
        
        @Schema(description = "消耗数量")
        private BigDecimal consumptionQuantity;
        
        @Schema(description = "消耗成本")
        private BigDecimal consumptionCost;
        
        @Schema(description = "成本占比(%)")
        private BigDecimal costRate;
    }
}
