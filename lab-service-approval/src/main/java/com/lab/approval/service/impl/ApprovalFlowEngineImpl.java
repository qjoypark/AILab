package com.lab.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApprovalFlowDefinition;
import com.lab.approval.dto.ApprovalRequest;
import com.lab.approval.entity.ApprovalFlowConfig;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.service.ApprovalFlowEngine;
import com.lab.approval.service.ApproverAssignmentService;
import com.lab.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批流程引擎实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalFlowEngineImpl implements ApprovalFlowEngine {
    
    private final ApprovalFlowConfigMapper flowConfigMapper;
    private final ApprovalRecordMapper recordMapper;
    private final ApproverAssignmentService assignmentService;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startApprovalFlow(Long applicationId, String applicationNo, ApprovalContext context) {
        log.info("启动审批流程: applicationId={}, applicationNo={}, context={}", 
                applicationId, applicationNo, context);
        
        // 1. 根据业务类型获取流程配置
        ApprovalFlowConfig flowConfig = getFlowConfigByBusinessType(context.getApplicationType());
        if (flowConfig == null) {
            throw new BusinessException("未找到对应的审批流程配置");
        }
        
        // 2. 解析流程定义
        ApprovalFlowDefinition flowDefinition = parseFlowDefinition(flowConfig.getFlowDefinition());
        if (flowDefinition.getLevels() == null || flowDefinition.getLevels().isEmpty()) {
            throw new BusinessException("审批流程配置错误：无审批层级");
        }
        
        // 3. 获取第一级审批人
        ApprovalFlowDefinition.ApprovalLevel firstLevel = flowDefinition.getLevels().get(0);
        Long firstApproverId = assignApprover(firstLevel, context);
        
        if (firstApproverId == null) {
            throw new BusinessException("无法分配第一级审批人");
        }
        
        log.info("审批流程启动成功: applicationId={}, firstApproverId={}", applicationId, firstApproverId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processApproval(ApprovalRequest request, Long approverId, String approverName) {
        log.info("处理审批: request={}, approverId={}, approverName={}", 
                request, approverId, approverName);
        
        // 1. 验证审批请求
        validateApprovalRequest(request);
        
        // 2. 获取当前审批层级
        Integer currentLevel = getCurrentApprovalLevel(request.getApplicationId());
        if (currentLevel == null) {
            throw new BusinessException("申请单不在审批流程中");
        }
        
        // 3. 验证审批人权限
        Long currentApproverId = getCurrentApproverId(request.getApplicationId());
        if (!approverId.equals(currentApproverId)) {
            throw new BusinessException("您不是当前审批人，无权审批");
        }
        
        // 4. 记录审批结果
        ApprovalRecord record = new ApprovalRecord();
        record.setApplicationId(request.getApplicationId());
        record.setApplicationNo(getApplicationNo(request.getApplicationId()));
        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setApprovalLevel(currentLevel);
        record.setApprovalResult(request.getApprovalResult());
        record.setApprovalOpinion(request.getApprovalOpinion());
        record.setApprovalTime(LocalDateTime.now());
        recordMapper.insert(record);
        
        // 5. 根据审批结果处理流转
        return handleApprovalResult(request, currentLevel);
    }
    
    @Override
    public Integer getCurrentApprovalLevel(Long applicationId) {
        // 查询最新的审批记录
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getApplicationId, applicationId)
               .orderByDesc(ApprovalRecord::getApprovalLevel)
               .last("LIMIT 1");
        
        ApprovalRecord latestRecord = recordMapper.selectOne(wrapper);
        
        if (latestRecord == null) {
            // 没有审批记录，说明在第一级
            return 1;
        }
        
        // 如果最后一次审批被拒绝，流程已终止
        if (latestRecord.getApprovalResult() == 2) {
            return null;
        }
        
        // 如果最后一次审批通过，检查是否还有下一级
        // 这里需要获取流程配置来判断
        // 简化处理：返回下一级
        return latestRecord.getApprovalLevel() + 1;
    }
    
    @Override
    public Long getCurrentApproverId(Long applicationId) {
        // 这里需要根据当前层级和流程配置获取审批人
        // 简化实现：从申请单表中获取current_approver_id
        // 实际应该查询material_application表
        return null; // TODO: 实现获取当前审批人逻辑
    }
    
    @Override
    public ApprovalFlowDefinition parseFlowDefinition(String flowDefinitionJson) {
        try {
            return objectMapper.readValue(flowDefinitionJson, ApprovalFlowDefinition.class);
        } catch (Exception e) {
            log.error("解析流程定义失败: {}", flowDefinitionJson, e);
            throw new BusinessException("流程定义格式错误");
        }
    }
    
    @Override
    public Long assignApprover(ApprovalFlowDefinition.ApprovalLevel level, ApprovalContext context) {
        return assignmentService.assignApprover(level.getApproverRole(), context);
    }
    
    /**
     * 根据业务类型获取流程配置
     */
    private ApprovalFlowConfig getFlowConfigByBusinessType(Integer businessType) {
        LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
               .eq(ApprovalFlowConfig::getStatus, 1)
               .last("LIMIT 1");
        return flowConfigMapper.selectOne(wrapper);
    }
    
    /**
     * 验证审批请求
     */
    private void validateApprovalRequest(ApprovalRequest request) {
        if (request.getApplicationId() == null) {
            throw new BusinessException("申请单ID不能为空");
        }
        if (request.getApprovalResult() == null) {
            throw new BusinessException("审批结果不能为空");
        }
        if (request.getApprovalResult() < 1 || request.getApprovalResult() > 3) {
            throw new BusinessException("审批结果无效");
        }
        if (request.getApprovalResult() == 3 && request.getTransferToUserId() == null) {
            throw new BusinessException("转审时必须指定目标用户");
        }
    }
    
    /**
     * 获取申请单号
     */
    private String getApplicationNo(Long applicationId) {
        // TODO: 从material_application表查询
        return "APP" + applicationId;
    }
    
    /**
     * 处理审批结果
     */
    private boolean handleApprovalResult(ApprovalRequest request, Integer currentLevel) {
        switch (request.getApprovalResult()) {
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
    private boolean handleApprovalPass(Long applicationId, Integer currentLevel) {
        // TODO: 检查是否还有下一级审批
        // 如果有下一级，分配下一级审批人
        // 如果没有下一级，标记申请单为审批通过
        log.info("审批通过: applicationId={}, currentLevel={}", applicationId, currentLevel);
        return true;
    }
    
    /**
     * 处理审批拒绝
     */
    private boolean handleApprovalReject(Long applicationId) {
        // TODO: 更新申请单状态为审批拒绝
        log.info("审批拒绝: applicationId={}", applicationId);
        return true;
    }
    
    /**
     * 处理审批转审
     */
    private boolean handleApprovalTransfer(Long applicationId, Long transferToUserId, Integer currentLevel) {
        // TODO: 更新当前审批人为转审目标用户
        log.info("审批转审: applicationId={}, transferToUserId={}, currentLevel={}", 
                applicationId, transferToUserId, currentLevel);
        return true;
    }
}
