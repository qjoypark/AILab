package com.lab.inventory.dto;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 出库单DTO
 */
@Data
public class StockOutDTO {
    
    private Long id;
    
    @NotNull(message = "出库类型不能为空")
    private Integer outType;
    
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    
    private Long applicationId;
    
    private Long receiverId;
    
    private String receiverName;
    
    private String receiverDept;
    
    @NotNull(message = "出库日期不能为空")
    private LocalDate outDate;
    
    @NotNull(message = "经手人ID不能为空")
    private Long operatorId;
    
    private String remark;
    
    @NotEmpty(message = "出库明细不能为空")
    @Valid
    private List<StockOutDetailDTO> items;
    
    @Data
    public static class StockOutDetailDTO {
        
        @NotNull(message = "药品ID不能为空")
        private Long materialId;
        
        private String batchNumber;
        
        @NotNull(message = "出库数量不能为空")
        private BigDecimal quantity;
        
        private Long storageLocationId;
    }
}
