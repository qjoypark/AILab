package com.lab.approval.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 危化品归还请求DTO
 */
@Data
public class HazardousReturnRequest {
    
    /**
     * 实际使用数量
     */
    @NotNull(message = "实际使用数量不能为空")
    @DecimalMin(value = "0", message = "实际使用数量不能为负数")
    private BigDecimal actualUsedQuantity;
    
    /**
     * 归还数量
     */
    @NotNull(message = "归还数量不能为空")
    @DecimalMin(value = "0", message = "归还数量不能为负数")
    private BigDecimal returnedQuantity;
    
    /**
     * 废弃数量
     */
    @NotNull(message = "废弃数量不能为空")
    @DecimalMin(value = "0", message = "废弃数量不能为负数")
    private BigDecimal wasteQuantity;
    
    /**
     * 归还备注
     */
    private String remark;
}
