package com.lab.approval.dto;

import lombok.Data;

/**
 * 审批请求DTO
 */
@Data
public class ApprovalRequest {
    
    /**
     * 申请单ID
     */
    private Long applicationId;
    
    /**
     * 审批结果: 1-通过, 2-拒绝, 3-转审
     */
    private Integer approvalResult;
    
    /**
     * 审批意见
     */
    private String approvalOpinion;
    
    /**
     * 转审目标用户ID (当approvalResult=3时必填)
     */
    private Long transferToUserId;
}
