package com.lab.approval.service;

import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApprovalFlowDefinition;
import com.lab.approval.dto.ApprovalRequest;

/**
 * 审批流程引擎接口
 */
public interface ApprovalFlowEngine {
    
    /**
     * 启动审批流程
     * 
     * @param applicationId 申请单ID
     * @param applicationNo 申请单号
     * @param context 审批上下文
     * @return 是否启动成功
     */
    boolean startApprovalFlow(Long applicationId, String applicationNo, ApprovalContext context);
    
    /**
     * 处理审批
     * 
     * @param request 审批请求
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @return 是否处理成功
     */
    boolean processApproval(ApprovalRequest request, Long approverId, String approverName);
    
    /**
     * 获取当前审批层级
     * 
     * @param applicationId 申请单ID
     * @return 当前审批层级，如果审批已完成返回null
     */
    Integer getCurrentApprovalLevel(Long applicationId);
    
    /**
     * 获取当前审批人ID
     * 
     * @param applicationId 申请单ID
     * @return 当前审批人ID，如果审批已完成返回null
     */
    Long getCurrentApproverId(Long applicationId);
    
    /**
     * 解析流程定义
     * 
     * @param flowDefinitionJson JSON格式的流程定义
     * @return 流程定义对象
     */
    ApprovalFlowDefinition parseFlowDefinition(String flowDefinitionJson);
    
    /**
     * 根据上下文自动分配审批人
     * 
     * @param level 审批层级定义
     * @param context 审批上下文
     * @return 审批人ID
     */
    Long assignApprover(ApprovalFlowDefinition.ApprovalLevel level, ApprovalContext context);
}
