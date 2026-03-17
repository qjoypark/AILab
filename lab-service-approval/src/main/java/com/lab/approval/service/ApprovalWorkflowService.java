package com.lab.approval.service;

import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApprovalRequest;
import com.lab.approval.entity.ApprovalRecord;

import java.util.List;

/**
 * 审批工作流服务接口
 * 提供完整的审批流程管理功能
 */
public interface ApprovalWorkflowService {
    
    /**
     * 初始化审批流程
     * 
     * @param applicationId 申请单ID
     * @param applicationNo 申请单号
     * @param context 审批上下文
     * @return 第一级审批人ID
     */
    Long initializeApprovalWorkflow(Long applicationId, String applicationNo, ApprovalContext context);
    
    /**
     * 执行审批操作
     * 
     * @param request 审批请求
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @return 审批结果: "APPROVED"(审批通过), "REJECTED"(审批拒绝), "NEXT_LEVEL"(流转到下一级), "TRANSFERRED"(已转审)
     */
    String executeApproval(ApprovalRequest request, Long approverId, String approverName);
    
    /**
     * 获取申请单的审批历史
     * 
     * @param applicationId 申请单ID
     * @return 审批记录列表
     */
    List<ApprovalRecord> getApprovalHistory(Long applicationId);
    
    /**
     * 检查用户是否为当前审批人
     * 
     * @param applicationId 申请单ID
     * @param userId 用户ID
     * @return 是否为当前审批人
     */
    boolean isCurrentApprover(Long applicationId, Long userId);
    
    /**
     * 获取当前审批层级信息
     * 
     * @param applicationId 申请单ID
     * @return 当前审批层级，如果审批已完成返回null
     */
    Integer getCurrentLevel(Long applicationId);
}
