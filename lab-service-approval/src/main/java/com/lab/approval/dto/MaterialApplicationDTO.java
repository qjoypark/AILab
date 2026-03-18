package com.lab.approval.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 领用申请详情DTO
 */
@Data
public class MaterialApplicationDTO {

    private Long id;

    private String applicationNo;

    private Long applicantId;

    private String applicantName;

    private String applicantDept;

    /**
     * 1-普通领用 2-危化品领用
     */
    private Integer applicationType;

    private String usagePurpose;

    private String usageLocation;

    private LocalDate expectedDate;

    /**
     * 1-待审批 2-审批中 3-审批通过 4-审批拒绝 5-已出库 6-已完成 7-已取消
     */
    private Integer status;

    /**
     * 0-未审批 1-审批中 2-审批通过 3-审批拒绝
     */
    private Integer approvalStatus;

    private Long currentApproverId;

    private String remark;

    private LocalDateTime createdTime;

    private List<MaterialApplicationItemDTO> items;

    private List<ApprovalRecordDTO> approvalRecords;

    /**
     * 出库流程状态: 0-未生成 1-出库流程中 2-已全部出库
     */
    private Integer stockOutFlowStatus;

    private String stockOutFlowStatusName;

    /**
     * 本申请生成的出库单号（逗号分隔）
     */
    private String stockOutOrderNos;

    /**
     * 本申请生成的出库单明细
     */
    private List<StockOutOrderInfoDTO> stockOutOrders;
}

