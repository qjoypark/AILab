package com.lab.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出库单摘要信息（用于申请详情追踪）
 */
@Data
public class StockOutOrderSummaryDTO {

    private Long id;

    private String outOrderNo;

    private Long warehouseId;

    private String warehouseName;

    /**
     * 1-待出库 2-已出库 3-已取消
     */
    private Integer status;

    private String statusName;

    private LocalDateTime createdTime;
}

