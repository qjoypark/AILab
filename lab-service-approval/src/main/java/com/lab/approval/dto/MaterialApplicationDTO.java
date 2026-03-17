package com.lab.approval.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 领用申请DTO
 */
@Data
public class MaterialApplicationDTO {
    
    /**
     * 申请单ID
     */
    private Long id;
    
    /**
     * 申请单号
     */
    private String applicationNo;
    
    /**
     * 申请人ID
     */
    private Long applicantId;
    
    /**
     * 申请人姓名
     */
    private String applicantName;
    
    /**
     * 申请部门
     */
    private String applicantDept;
    
    /**
     * 申请类型: 1-普通领用, 2-危化品领用
     */
    private Integer applicationType;
    
    /**
     * 用途说明
     */
    private String usagePurpose;
    
    /**
     * 使用地点
     */
    private String usageLocation;
    
    /**
     * 期望领用日期
     */
    private LocalDate expectedDate;
    
    /**
     * 状态: 1-待审批, 2-审批中, 3-审批通过, 4-审批拒绝, 5-已出库, 6-已完成, 7-已取消
     */
    private Integer status;
    
    /**
     * 审批状态: 0-未审批, 1-审批中, 2-审批通过, 3-审批拒绝
     */
    private Integer approvalStatus;
    
    /**
     * 当前审批人ID
     */
    private Long currentApproverId;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 申请明细列表
     */
    private List<MaterialApplicationItemDTO> items;
    
    /**
     * 审批记录列表
     */
    private List<ApprovalRecordDTO> approvalRecords;
}
