package com.lab.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApprovalFlowDefinition;
import com.lab.approval.dto.ApprovalRequest;
import com.lab.approval.entity.ApprovalFlowConfig;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.service.ApprovalFlowEngine;
import com.lab.approval.service.ApprovalWorkflowService;
import com.lab.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批工作流服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {
    
    private final ApprovalFlowEngine flowEngine;
    private final ApprovalFlowConfigMapper flowConfigMapper;
    private final ApprovalRecordMapper recordMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long initializeApprovalWorkflow(Long applicationId, String applicationNo, ApprovalContext context) {
        log.info("初始化审批流程: applicationId={}, applicationNo={}", applicationId, applicationNo);
        
        // 1. 获取流程配置
        ApprovalFlowConfig flowConfig = getFlowConfig(context.getApplicationType());
        ApprovalFlowDefinition flowDef = flowEngine.parseFlowDefinition(flowConfig.getFlowDefinition());
        
        // 2. 分配第一级审批人
        ApprovalFlowDefinition.ApprovalLevel firstLevel = flowDef.getLevels().get(0);
        Long firstApproverId = flowEngine.assignApprover(firstLevel, context);
        
        if (firstApproverId == null) {
            throw new BusinessException("无法分配第一级审批人");
        }
        
        log.info("审批流程初始化成功: applicationId={}, firstApproverId={}", applicationId, firstApproverId);
        return firstApproverId;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String executeApproval(ApprovalRequest request, Long approverId, String approverName) {
        log.info("执行审批: applicationId={}, approverId={}, result={}", 
                request.getApplicationId(), approverId, request.getApprovalResult());
        
        // 1. 验证审批权限
        if (!isCurrentApprover(request.getApplicationId(), approverId)) {
            throw new BusinessException("您不是当前审批人，无权审批");
        }
        
        // 2. 获取当前层级
        Integer currentLevel = getCurrentLevel(request.getApplicationId());
        if (currentLevel == null) {
            throw new BusinessException("申请单不在审批流程中");
        }
        
        // 3. 记录审批结果
        ApprovalRecord record = createApprovalRecord(request, approverId, approverName, currentLevel);
        recordMapper.insert(record);
        
        // 4. 根据审批结果处理流转
        return handleApprovalFlow(request, currentLevel);
    }
    
    @Override
    public List<ApprovalRecord> getApprovalHistory(Long applicationId) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getApplicationId, applicationId)
               .orderByAsc(ApprovalRecord::getApprovalLevel);
        return recordMapper.selectList(wrapper);
    }
    
    @Override
    public boolean isCurrentApprover(Long applicationId, Long userId) {
        // TODO: 实际应该从material_application表查询current_approver_id
        // 这里简化实现
        Long currentApproverId = flowEngine.getCurrentApproverId(applicationId);
        return userId.equals(currentApproverId);
    }
    
    @Override
    public Integer getCurrentLevel(Long applicationId) {
        return flowEngine.getCurrentApprovalLevel(applicationId);
    }
    
    /**
     * 获取流程配置
     */
    private ApprovalFlowConfig getFlowConfig(Integer businessType) {
        LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
               .eq(ApprovalFlowConfig::getStatus, 1)
               .last("LIMIT 1");
        
        ApprovalFlowConfig config = flowConfigMapper.selectOne(wrapper);
        if (config == null) {
            throw new BusinessException("未找到对应的审批流程配置");
        }
        return config;
    }
    
    /**
     * 创建审批记录
     */
    private ApprovalRecord createApprovalRecord(ApprovalRequest request, Long approverId, 
                                                String approverName, Integer currentLevel) {
        ApprovalRecord record = new ApprovalRecord();
        record.setApplicationId(request.getApplicationId());
        record.setApplicationNo("APP" + request.getApplicationId()); // TODO: 从申请单表获取
        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setApprovalLevel(currentLevel);
        record.setApprovalResult(request.getApprovalResult());
        record.setApprovalOpinion(request.getApprovalOpinion());
        record.setApprovalTime(LocalDateTime.now());
        return record;
    }
    
    /**
     * 处理审批流转
     */
    private String handleApprovalFlow(ApprovalRequest request, Integer currentLevel) {
        Integer approvalResult = request.getApprovalResult();
        
        // 1-通过, 2-拒绝, 3-转审
        switch (approvalResult) {
            case 1: // 通过
                return handleApprovalPass(request.getApplicationId(), currentLevel);
            case 2: // 拒绝
                return handleApprovalReject(request.getApplicationId());
            case 3: // 转审
                return handleApprovalTransfer(request.getApplicationId(), 
                        request.getTransferToUserId(), currentLevel);
            default:
                throw new BusinessException("未知的审批结果");
        }
    }
    
    /**
     * 处理审批通过
     */
    private String handleApprovalPass(Long applicationId, Integer currentLevel) {
        // TODO: 获取流程配置，检查是否还有下一级
        // 如果有下一级，分配下一级审批人并返回"NEXT_LEVEL"
        // 如果没有下一级，更新申请单状态为审批通过并返回"APPROVED"
        
        log.info("审批通过: applicationId={}, currentLevel={}", applicationId, currentLevel);
        
        // 简化实现：假设最多3级审批
        if (currentLevel < 3) {
            // TODO: 分配下一级审批人
            // TODO: 更新material_application表的current_approver_id
            return "NEXT_LEVEL";
        } else {
            // TODO: 更新material_application表的status和approval_status
            return "APPROVED";
        }
    }
    
    /**
     * 处理审批拒绝
     */
    private String handleApprovalReject(Long applicationId) {
        // TODO: 更新material_application表的status为审批拒绝
        log.info("审批拒绝: applicationId={}", applicationId);
        return "REJECTED";
    }
    
    /**
     * 处理审批转审
     */
    private String handleApprovalTransfer(Long applicationId, Long transferToUserId, Integer currentLevel) {
        // TODO: 更新material_application表的current_approver_id为转审目标用户
        log.info("审批转审: applicationId={}, transferToUserId={}, currentLevel={}", 
                applicationId, transferToUserId, currentLevel);
        return "TRANSFERRED";
    }
}
