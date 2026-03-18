package com.lab.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申请关联出库单信息
 */
@Data
public class StockOutOrderInfoDTO {

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

