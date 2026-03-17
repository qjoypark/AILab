package com.lab.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批记录DTO
 */
@Data
public class ApprovalRecordDTO {
    
    /**
     * 记录ID
     */
    private Long id;
    
    /**
     * 审批人姓名
     */
    private String approverName;
    
    /**
     * 审批层级
     */
    private Integer approvalLevel;
    
    /**
     * 审批结果: 1-通过, 2-拒绝
     */
    private Integer approvalResult;
    
    /**
     * 审批意见
     */
    private String approvalOpinion;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
}
