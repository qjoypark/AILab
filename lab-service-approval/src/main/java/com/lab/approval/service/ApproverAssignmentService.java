package com.lab.approval.service;

import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApproverCandidateDTO;

import java.util.List;

/**
 * 审批人自动分配服务接口
 */
public interface ApproverAssignmentService {

    /**
     * 根据角色和上下文自动分配审批人
     *
     * @param approverRole 审批角色
     * @param context 审批上下文
     * @return 审批人ID
     */
    Long assignApprover(String approverRole, ApprovalContext context);

    /**
     * 获取候选审批人列表（按角色）
     *
     * @param approverRole 流程中配置的审批角色
     * @param context 审批上下文
     * @return 候选审批人列表
     */
    List<ApproverCandidateDTO> listApproverCandidates(String approverRole, ApprovalContext context);

    /**
     * 解析并返回当前业务下最终生效的审批角色编码。
     * 说明：流程配置角色可能会被业务规则覆盖（例如统一改为 CENTER_ADMIN）。
     *
     * @param approverRole 流程中配置的审批角色
     * @param context 审批上下文
     * @return 最终生效角色编码（大写），无可用角色返回 null
     */
    String resolveEffectiveRoleCode(String approverRole, ApprovalContext context);
}
