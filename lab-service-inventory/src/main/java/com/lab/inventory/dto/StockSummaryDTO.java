package com.lab.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存汇总报表DTO
 */
@Data
@Schema(description = "库存汇总报表")
public class StockSummaryDTO {
    
    @Schema(description = "库存总价值")
    private BigDecimal totalValue;
    
    @Schema(description = "分类汇总列表")
    private List<CategorySummary> categories;
    
    /**
     * 分类汇总
     */
    @Data
    @Schema(description = "分类汇总")
    public static class CategorySummary {
        
        @Schema(description = "分类ID")
        private Long categoryId;
        
        @Schema(description = "分类名称")
        private String categoryName;
        
        @Schema(description = "物品数量")
        private Integer itemCount;
        
        @Schema(description = "总库存数量")
        private BigDecimal totalQuantity;
        
        @Schema(description = "总价值")
        private BigDecimal totalValue;
        
        @Schema(description = "价值占比(%)")
        private BigDecimal valuePercentage;
    }
}
