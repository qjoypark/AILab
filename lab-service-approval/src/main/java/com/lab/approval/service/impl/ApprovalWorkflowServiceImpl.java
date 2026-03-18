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

        ApprovalFlowConfig flowConfig = getFlowConfig(context.getApplicationType());
        ApprovalFlowDefinition flowDefinition = flowEngine.parseFlowDefinition(flowConfig.getFlowDefinition());

        ApprovalFlowDefinition.ApprovalLevel firstLevel = flowDefinition.getLevels().get(0);
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

        if (!isCurrentApprover(request.getApplicationId(), approverId)) {
            throw new BusinessException("您不是当前审批人，无权审批");
        }

        Integer currentLevel = getCurrentLevel(request.getApplicationId());
        if (currentLevel == null) {
            throw new BusinessException("申请单不在审批流程中");
        }

        ApprovalRecord record = createApprovalRecord(request, approverId, approverName, currentLevel);
        recordMapper.insert(record);

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
        Long currentApproverId = flowEngine.getCurrentApproverId(applicationId);
        return userId.equals(currentApproverId);
    }

    @Override
    public Integer getCurrentLevel(Long applicationId) {
        return flowEngine.getCurrentApprovalLevel(applicationId);
    }

    /**
     * 获取流程配置；若数据缺失自动补齐默认配置
     */
    private ApprovalFlowConfig getFlowConfig(Integer businessType) {
        Integer safeBusinessType = businessType == null ? 1 : businessType;
        LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowConfig::getBusinessType, safeBusinessType)
                .eq(ApprovalFlowConfig::getStatus, 1)
                .last("LIMIT 1");

        ApprovalFlowConfig config = flowConfigMapper.selectOne(wrapper);
        if (config != null) {
            return config;
        }

        log.warn("未找到审批流程配置，尝试自动创建默认流程: businessType={}", safeBusinessType);
        return createDefaultFlowConfig(safeBusinessType);
    }

    /**
     * 自动创建默认审批流程配置，防止初始化数据缺失导致流程不可用
     */
    private ApprovalFlowConfig createDefaultFlowConfig(Integer businessType) {
        ApprovalFlowConfig flowConfig = new ApprovalFlowConfig();
        flowConfig.setBusinessType(businessType);
        flowConfig.setStatus(1);
        flowConfig.setCreatedTime(LocalDateTime.now());
        flowConfig.setUpdatedTime(LocalDateTime.now());

        if (businessType != null && businessType == 2) {
            flowConfig.setFlowCode("HAZARDOUS_APPLY");
            flowConfig.setFlowName("危化品领用审批流程");
            flowConfig.setFlowDefinition(
                    "{\"levels\":[{\"level\":1,\"approverRole\":\"LAB_MANAGER\",\"approverName\":\"实验室负责人\"}," +
                            "{\"level\":2,\"approverRole\":\"CENTER_ADMIN\",\"approverName\":\"中心管理员\"}," +
                            "{\"level\":3,\"approverRole\":\"ADMIN\",\"approverName\":\"安全管理员\"}]}"
            );
        } else {
            flowConfig.setFlowCode("NORMAL_APPLY");
            flowConfig.setFlowName("普通领用审批流程");
            flowConfig.setFlowDefinition(
                    "{\"levels\":[{\"level\":1,\"approverRole\":\"LAB_MANAGER\",\"approverName\":\"实验室负责人\"}]}"
            );
        }

        try {
            flowConfigMapper.insert(flowConfig);
            return flowConfig;
        } catch (Exception e) {
            log.warn("默认审批流程创建失败，尝试重新查询已存在配置: businessType={}", businessType, e);
            LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
                    .eq(ApprovalFlowConfig::getStatus, 1)
                    .last("LIMIT 1");
            ApprovalFlowConfig existingConfig = flowConfigMapper.selectOne(wrapper);
            if (existingConfig != null) {
                return existingConfig;
            }
            throw new BusinessException("未找到对应的审批流程配置，请先在系统管理中配置审批流程");
        }
    }

    /**
     * 创建审批记录
     */
    private ApprovalRecord createApprovalRecord(ApprovalRequest request, Long approverId,
                                                String approverName, Integer currentLevel) {
        ApprovalRecord record = new ApprovalRecord();
        record.setApplicationId(request.getApplicationId());
        record.setApplicationNo("APP" + request.getApplicationId());
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

        switch (approvalResult) {
            case 1:
                return handleApprovalPass(request.getApplicationId(), currentLevel);
            case 2:
                return handleApprovalReject(request.getApplicationId());
            case 3:
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
        log.info("审批通过: applicationId={}, currentLevel={}", applicationId, currentLevel);

        if (currentLevel < 3) {
            return "NEXT_LEVEL";
        }
        return "APPROVED";
    }

    /**
     * 处理审批拒绝
     */
    private String handleApprovalReject(Long applicationId) {
        log.info("审批拒绝: applicationId={}", applicationId);
        return "REJECTED";
    }

    /**
     * 处理审批转审
     */
    private String handleApprovalTransfer(Long applicationId, Long transferToUserId, Integer currentLevel) {
        log.info("审批转审: applicationId={}, transferToUserId={}, currentLevel={}",
                applicationId, transferToUserId, currentLevel);
        return "TRANSFERRED";
    }
}

