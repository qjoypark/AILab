package com.lab.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 危化品归还入库请求
 */
@Data
public class HazardousReturnStockInRequest {

    @NotNull(message = "药品ID不能为空")
    private Long materialId;

    @NotNull(message = "归还数量不能为空")
    @DecimalMin(value = "0.01", message = "归还数量必须大于0")
    private BigDecimal returnQuantity;

    private String remark;
}
