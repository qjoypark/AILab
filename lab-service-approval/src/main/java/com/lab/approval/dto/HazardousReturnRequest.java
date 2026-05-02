package com.lab.approval.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Return request DTO (historical class name kept).
 */
@Data
public class HazardousReturnRequest {

    /**
     * Actual used quantity. Optional; auto-calculated when absent.
     */
    @DecimalMin(value = "0", message = "actualUsedQuantity must be >= 0")
    private BigDecimal actualUsedQuantity;

    /**
     * Returned quantity.
     */
    @NotNull(message = "returnedQuantity is required")
    @DecimalMin(value = "0", message = "returnedQuantity must be >= 0")
    private BigDecimal returnedQuantity;

    /**
     * Waste quantity. Optional; defaults to 0 when absent.
     */
    @DecimalMin(value = "0", message = "wasteQuantity must be >= 0")
    private BigDecimal wasteQuantity;

    /**
     * Remark.
     */
    private String remark;
}
