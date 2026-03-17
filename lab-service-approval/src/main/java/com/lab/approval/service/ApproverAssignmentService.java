package com.lab.approval.service;

import com.lab.approval.dto.ApprovalContext;

/**
 * 审批人自动分配服务接口
 */
public interface ApproverAssignmentService {
    
    /**
     * 根据角色和上下文自动分配审批人
     * 
     * @param approverRole 审批人角色
     * @param context 审批上下文
     * @return 审批人ID
     */
    Long assignApprover(String approverRole, ApprovalContext context);
}
