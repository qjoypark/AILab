package com.lab.inventory.dto;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 入库单DTO
 */
@Data
public class StockInDTO {
    
    private Long id;
    
    @NotNull(message = "入库类型不能为空")
    private Integer inType;
    
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    
    private Long supplierId;
    
    @NotNull(message = "入库日期不能为空")
    private LocalDate inDate;
    
    @NotNull(message = "经手人ID不能为空")
    private Long operatorId;
    
    private String remark;
    
    private String attachmentUrl;
    
    @NotEmpty(message = "入库明细不能为空")
    @Valid
    private List<StockInDetailDTO> items;
    
    @Data
    public static class StockInDetailDTO {
        
        @NotNull(message = "药品ID不能为空")
        private Long materialId;
        
        private String batchNumber;
        
        @NotNull(message = "入库数量不能为空")
        private BigDecimal quantity;
        
        private BigDecimal unitPrice;
        
        private LocalDate productionDate;
        
        private LocalDate expireDate;
        
        private Long storageLocationId;
    }
}
