package com.lab.inventory.dto;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 库存盘点DTO
 */
@Data
public class StockCheckDTO {
    
    private Long id;
    
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    
    @NotNull(message = "盘点日期不能为空")
    private LocalDate checkDate;
    
    @NotNull(message = "盘点人ID不能为空")
    private Long checkerId;
    
    private String remark;
    
    @Valid
    private List<StockCheckDetailDTO> items;
    
    @Data
    public static class StockCheckDetailDTO {
        
        @NotNull(message = "药品ID不能为空")
        private Long materialId;
        
        private String batchNumber;
        
        @NotNull(message = "账面数量不能为空")
        private BigDecimal bookQuantity;
        
        @NotNull(message = "实际数量不能为空")
        private BigDecimal actualQuantity;
        
        private String diffReason;
        
        private Long storageLocationId;
    }
}
