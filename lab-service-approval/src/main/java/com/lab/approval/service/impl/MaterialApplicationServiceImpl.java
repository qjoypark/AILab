package com.lab.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.client.InventoryClient;
import com.lab.approval.client.MaterialClient;
import com.lab.approval.dto.*;
import com.lab.approval.entity.ApprovalFlowConfig;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.entity.MaterialApplicationItem;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.mapper.MaterialApplicationItemMapper;
import com.lab.approval.mapper.MaterialApplicationMapper;
import com.lab.approval.service.ApprovalWorkflowService;
import com.lab.approval.service.MaterialApplicationService;
import com.lab.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 领用申请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialApplicationServiceImpl implements MaterialApplicationService {
    
    private final MaterialApplicationMapper applicationMapper;
    private final MaterialApplicationItemMapper itemMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalFlowConfigMapper flowConfigMapper;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final InventoryClient inventoryClient;
    private final MaterialClient materialClient;
    private final com.lab.approval.client.UserClient userClient;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApplication(CreateApplicationRequest request, Long applicantId, 
                                   String applicantName, String applicantDept) {
        log.info("创建领用申请: applicantId={}, applicationType={}", applicantId, request.getApplicationType());
        
        // 1. 验证申请明细
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("申请明细不能为空");
        }
        
        // 2. 危化品申请特殊验证
        if (request.getApplicationType() != null && request.getApplicationType() == 2) {
            // 2.1 验证申请人安全资质
            boolean hasSafetyCert = userClient.checkSafetyCertification(applicantId);
            if (!hasSafetyCert) {
                throw new BusinessException("安全资质未通过或已过期，无法申请危化品");
            }
            
            // 2.2 验证必填字段：用途说明
            if (request.getUsagePurpose() == null || request.getUsagePurpose().trim().isEmpty()) {
                throw new BusinessException("危化品申请必须填写用途说明");
            }
            
            // 2.3 验证必填字段：使用地点
            if (request.getUsageLocation() == null || request.getUsageLocation().trim().isEmpty()) {
                throw new BusinessException("危化品申请必须填写使用地点");
            }
            
            log.info("危化品申请验证通过: applicantId={}, usagePurpose={}, usageLocation={}", 
                applicantId, request.getUsagePurpose(), request.getUsageLocation());
        }
        
        // 3. 检查库存是否充足
        for (CreateApplicationRequest.ApplicationItemRequest item : request.getItems()) {
            if (item.getApplyQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("申请数量必须大于0");
            }
            
            boolean stockAvailable = inventoryClient.checkStockAvailability(
                item.getMaterialId(), 
                item.getApplyQuantity()
            );
            
            if (!stockAvailable) {
                MaterialInfo materialInfo = materialClient.getMaterialInfo(item.getMaterialId());
                BigDecimal availableStock = inventoryClient.getAvailableStock(item.getMaterialId());
                throw new BusinessException(
                    String.format("药品 %s 库存不足，当前可用库存: %s %s，申请数量: %s %s",
                        materialInfo.getMaterialName(),
                        availableStock,
                        materialInfo.getUnit(),
                        item.getApplyQuantity(),
                        materialInfo.getUnit()
                    )
                );
            }
        }
        
        // 4. 创建申请单
        MaterialApplication application = new MaterialApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setApplicantId(applicantId);
        application.setApplicantName(applicantName);
        application.setApplicantDept(applicantDept);
        application.setApplicationType(request.getApplicationType());
        application.setUsagePurpose(request.getUsagePurpose());
        application.setUsageLocation(request.getUsageLocation());
        application.setExpectedDate(request.getExpectedDate());
        application.setStatus(1); // 待审批
        application.setApprovalStatus(0); // 未审批
        application.setRemark(request.getRemark());
        application.setCreatedBy(applicantId);
        application.setCreatedTime(LocalDateTime.now());
        
        applicationMapper.insert(application);
        
        // 5. 创建申请明细
        for (CreateApplicationRequest.ApplicationItemRequest itemRequest : request.getItems()) {
            MaterialInfo materialInfo = materialClient.getMaterialInfo(itemRequest.getMaterialId());
            
            MaterialApplicationItem item = new MaterialApplicationItem();
            item.setApplicationId(application.getId());
            item.setMaterialId(itemRequest.getMaterialId());
            item.setMaterialName(materialInfo.getMaterialName());
            item.setSpecification(materialInfo.getSpecification());
            item.setUnit(materialInfo.getUnit());
            item.setApplyQuantity(itemRequest.getApplyQuantity());
            item.setRemark(itemRequest.getRemark());
            item.setCreatedTime(LocalDateTime.now());
            
            itemMapper.insert(item);
        }
        
        // 6. 启动审批流程
        ApprovalContext context = new ApprovalContext();
        context.setBusinessType(request.getApplicationType()); // 1-普通领用, 2-危化品领用
        context.setApplicantId(applicantId);
        context.setApplicantDept(applicantDept);
        
        Long firstApproverId = approvalWorkflowService.initializeApprovalWorkflow(
            application.getId(),
            application.getApplicationNo(),
            context
        );
        
        // 7. 更新申请单状态和当前审批人
        application.setStatus(2); // 审批中
        application.setApprovalStatus(1); // 审批中
        application.setCurrentApproverId(firstApproverId);
        applicationMapper.updateById(application);
        
        log.info("领用申请创建成功: applicationId={}, applicationNo={}", 
            application.getId(), application.getApplicationNo());
        
        return application.getId();
    }
    
    @Override
    public Page<MaterialApplication> listApplications(int page, int size, Integer status, 
                                                       Integer applicationType, LocalDate startDate, LocalDate endDate) {
        log.info("查询申请列表: page={}, size={}, status={}, applicationType={}", 
            page, size, status, applicationType);
        
        Page<MaterialApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MaterialApplication> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(MaterialApplication::getStatus, status);
        }
        if (applicationType != null) {
            wrapper.eq(MaterialApplication::getApplicationType, applicationType);
        }
        if (startDate != null) {
            wrapper.ge(MaterialApplication::getCreatedTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(MaterialApplication::getCreatedTime, endDate.plusDays(1).atStartOfDay());
        }
        
        wrapper.orderByDesc(MaterialApplication::getCreatedTime);
        
        return applicationMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public MaterialApplicationDTO getApplicationDetail(Long id) {
        log.info("查询申请详情: id={}", id);
        
        // 1. 查询申请单
        MaterialApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("申请单不存在");
        }
        
        // 2. 查询申请明细
        LambdaQueryWrapper<MaterialApplicationItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MaterialApplicationItem::getApplicationId, id);
        List<MaterialApplicationItem> items = itemMapper.selectList(itemWrapper);
        
        // 3. 查询审批记录
        List<ApprovalRecord> approvalRecords = approvalWorkflowService.getApprovalHistory(id);
        
        // 4. 组装DTO
        MaterialApplicationDTO dto = new MaterialApplicationDTO();
        dto.setId(application.getId());
        dto.setApplicationNo(application.getApplicationNo());
        dto.setApplicantId(application.getApplicantId());
        dto.setApplicantName(application.getApplicantName());
        dto.setApplicantDept(application.getApplicantDept());
        dto.setApplicationType(application.getApplicationType());
        dto.setUsagePurpose(application.getUsagePurpose());
        dto.setUsageLocation(application.getUsageLocation());
        dto.setExpectedDate(application.getExpectedDate());
        dto.setStatus(application.getStatus());
        dto.setApprovalStatus(application.getApprovalStatus());
        dto.setCurrentApproverId(application.getCurrentApproverId());
        dto.setRemark(application.getRemark());
        dto.setCreatedTime(application.getCreatedTime());
        
        // 转换申请明细
        List<MaterialApplicationItemDTO> itemDTOs = items.stream().map(item -> {
            MaterialApplicationItemDTO itemDTO = new MaterialApplicationItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setMaterialId(item.getMaterialId());
            itemDTO.setMaterialName(item.getMaterialName());
            itemDTO.setSpecification(item.getSpecification());
            itemDTO.setUnit(item.getUnit());
            itemDTO.setApplyQuantity(item.getApplyQuantity());
            itemDTO.setApprovedQuantity(item.getApprovedQuantity());
            itemDTO.setActualQuantity(item.getActualQuantity());
            itemDTO.setRemark(item.getRemark());
            return itemDTO;
        }).collect(Collectors.toList());
        dto.setItems(itemDTOs);
        
        // 转换审批记录
        List<ApprovalRecordDTO> recordDTOs = approvalRecords.stream().map(record -> {
            ApprovalRecordDTO recordDTO = new ApprovalRecordDTO();
            recordDTO.setId(record.getId());
            recordDTO.setApproverName(record.getApproverName());
            recordDTO.setApprovalLevel(record.getApprovalLevel());
            recordDTO.setApprovalResult(record.getApprovalResult());
            recordDTO.setApprovalOpinion(record.getApprovalOpinion());
            recordDTO.setApprovalTime(record.getApprovalTime());
            return recordDTO;
        }).collect(Collectors.toList());
        dto.setApprovalRecords(recordDTOs);
        
        return dto;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApplication(Long id, Long userId) {
        log.info("取消申请: id={}, userId={}", id, userId);
        
        MaterialApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("申请单不存在");
        }
        
        // 只有待审批和审批中的申请可以取消
        if (application.getStatus() != 1 && application.getStatus() != 2) {
            throw new BusinessException("当前状态不允许取消");
        }
        
        // 只有申请人可以取消
        if (!application.getApplicantId().equals(userId)) {
            throw new BusinessException("只有申请人可以取消申请");
        }
        
        application.setStatus(7); // 已取消
        application.setUpdatedBy(userId);
        application.setUpdatedTime(LocalDateTime.now());
        
        applicationMapper.updateById(application);
        
        log.info("申请已取消: id={}", id);
    }
    
    @Override
    public void updateApplicationStatus(Long id, Integer status) {
        log.info("更新申请状态: id={}, status={}", id, status);
        
        MaterialApplication application = new MaterialApplication();
        application.setId(id);
        application.setStatus(status);
        application.setUpdatedTime(LocalDateTime.now());
        
        applicationMapper.updateById(application);
    }
    
    @Override
    public void updateApprovalStatus(Long id, Integer approvalStatus) {
        log.info("更新审批状态: id={}, approvalStatus={}", id, approvalStatus);
        
        MaterialApplication application = new MaterialApplication();
        application.setId(id);
        application.setApprovalStatus(approvalStatus);
        application.setUpdatedTime(LocalDateTime.now());
        
        applicationMapper.updateById(application);
    }
    
    @Override
    public void updateCurrentApprover(Long id, Long approverId) {
        log.info("更新当前审批人: id={}, approverId={}", id, approverId);
        
        MaterialApplication application = new MaterialApplication();
        application.setId(id);
        application.setCurrentApproverId(approverId);
        application.setUpdatedTime(LocalDateTime.now());
        
        applicationMapper.updateById(application);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processApproval(Long id, com.lab.approval.dto.ApprovalProcessRequest request, 
                               Long approverId, String approverName) {
        log.info("处理审批: id={}, approverId={}, result={}", id, approverId, request.getApprovalResult());
        
        // 1. 查询申请单
        MaterialApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("申请单不存在");
        }
        
        // 2. 验证申请单状态
        if (application.getStatus() != 2) {
            throw new BusinessException("申请单不在审批中状态");
        }
        
        // 3. 验证审批人权限
        if (!approverId.equals(application.getCurrentApproverId())) {
            throw new BusinessException("您不是当前审批人，无权审批");
        }
        
        // 4. 获取当前审批层级
        List<ApprovalRecord> existingRecords = approvalWorkflowService.getApprovalHistory(id);
        int currentLevel = existingRecords.size() + 1;
        
        // 5. 如果审批通过且提供了批准数量，更新申请明细的批准数量
        if (request.getApprovalResult() == 1 && request.getApprovedQuantities() != null) {
            for (com.lab.approval.dto.ApprovalProcessRequest.ApprovedQuantityItem item : request.getApprovedQuantities()) {
                MaterialApplicationItem applicationItem = itemMapper.selectById(item.getItemId());
                if (applicationItem == null || !applicationItem.getApplicationId().equals(id)) {
                    throw new BusinessException("申请明细不存在或不属于该申请单");
                }
                
                // 验证批准数量不能超过申请数量
                if (item.getApprovedQuantity().compareTo(applicationItem.getApplyQuantity()) > 0) {
                    throw new BusinessException(
                        String.format("药品 %s 的批准数量不能超过申请数量", applicationItem.getMaterialName())
                    );
                }
                
                // 验证批准数量不能为负数
                if (item.getApprovedQuantity().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException("批准数量不能为负数");
                }
                
                // 更新批准数量
                applicationItem.setApprovedQuantity(item.getApprovedQuantity());
                itemMapper.updateById(applicationItem);
            }
        }
        
        // 6. 记录审批意见到approval_record表
        ApprovalRecord record = new ApprovalRecord();
        record.setApplicationId(id);
        record.setApplicationNo(application.getApplicationNo());
        record.setApproverId(approverId);
        record.setApproverName(approverName);
        record.setApprovalLevel(currentLevel);
        record.setApprovalResult(request.getApprovalResult());
        record.setApprovalOpinion(request.getApprovalOpinion());
        record.setApprovalTime(LocalDateTime.now());
        record.setCreatedTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
        
        // 7. 根据审批结果处理流转
        if (request.getApprovalResult() == 1) {
            // 审批通过，检查是否还有下一级
            handleApprovalPass(application, currentLevel, approverId);
        } else if (request.getApprovalResult() == 2) {
            // 审批拒绝，终止流程
            handleApprovalReject(application);
        } else {
            throw new BusinessException("无效的审批结果");
        }
        
        log.info("审批处理完成: id={}, result={}", id, request.getApprovalResult());
    }
    
    /**
     * 处理审批通过
     */
    private void handleApprovalPass(MaterialApplication application, int currentLevel, Long approverId) {
        log.info("处理审批通过: applicationId={}, currentLevel={}", application.getId(), currentLevel);
        
        // 获取流程配置
        ApprovalContext context = new ApprovalContext();
        context.setBusinessType(application.getApplicationType());
        context.setApplicantId(application.getApplicantId());
        context.setApplicantDept(application.getApplicantDept());
        
        // 检查是否还有下一级审批
        boolean hasNextLevel = checkHasNextLevel(application.getApplicationType(), currentLevel);
        
        if (hasNextLevel) {
            // 流转到下一级
            Long nextApproverId = assignNextLevelApprover(application.getApplicationType(), currentLevel + 1, context);
            
            if (nextApproverId == null) {
                throw new BusinessException("无法分配下一级审批人");
            }
            
            application.setCurrentApproverId(nextApproverId);
            application.setApprovalStatus(1); // 审批中
            application.setStatus(2); // 审批中
            application.setUpdatedTime(LocalDateTime.now());
            applicationMapper.updateById(application);
            
            // TODO: 发送审批通知给下一级审批人
            log.info("流转到下一级审批: applicationId={}, nextLevel={}, nextApproverId={}", 
                application.getId(), currentLevel + 1, nextApproverId);
        } else {
            // 所有审批通过，标记为审批完成
            application.setApprovalStatus(2); // 审批通过
            application.setStatus(3); // 审批通过
            application.setCurrentApproverId(null);
            application.setUpdatedTime(LocalDateTime.now());
            applicationMapper.updateById(application);
            
            // 审批通过后自动创建出库单
            try {
                Long stockOutId = inventoryClient.createStockOutOrder(application.getId());
                if (stockOutId != null) {
                    log.info("审批通过后自动创建出库单成功: applicationId={}, stockOutId={}", 
                        application.getId(), stockOutId);
                } else {
                    log.warn("审批通过后自动创建出库单失败: applicationId={}", application.getId());
                }
            } catch (Exception e) {
                log.error("审批通过后自动创建出库单异常: applicationId={}", application.getId(), e);
                // 不影响审批流程，只记录日志
            }
            
            // TODO: 发送审批通过通知给申请人
            log.info("审批流程完成: applicationId={}", application.getId());
        }
    }
    
    /**
     * 处理审批拒绝
     */
    private void handleApprovalReject(MaterialApplication application) {
        log.info("处理审批拒绝: applicationId={}", application.getId());
        
        application.setApprovalStatus(3); // 审批拒绝
        application.setStatus(4); // 审批拒绝
        application.setCurrentApproverId(null);
        application.setUpdatedTime(LocalDateTime.now());
        applicationMapper.updateById(application);
        
        // TODO: 发送审批拒绝通知给申请人
        log.info("审批已拒绝: applicationId={}", application.getId());
    }
    
    /**
     * 检查是否还有下一级审批
     */
    private boolean checkHasNextLevel(Integer businessType, int currentLevel) {
        try {
            // 获取流程配置
            LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
                   .eq(ApprovalFlowConfig::getStatus, 1)
                   .last("LIMIT 1");
            
            ApprovalFlowConfig config = flowConfigMapper.selectOne(wrapper);
            if (config == null) {
                // 如果没有配置，默认只有一级审批
                return false;
            }
            
            // 解析流程定义
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ApprovalFlowDefinition flowDef = objectMapper.readValue(
                config.getFlowDefinition(), 
                ApprovalFlowDefinition.class
            );
            
            // 检查是否还有下一级
            return flowDef.getLevels() != null && flowDef.getLevels().size() > currentLevel;
        } catch (Exception e) {
            log.error("检查下一级审批失败", e);
            // 出错时默认没有下一级
            return false;
        }
    }
    
    /**
     * 分配下一级审批人
     */
    private Long assignNextLevelApprover(Integer businessType, int nextLevel, ApprovalContext context) {
        try {
            // 获取流程配置
            LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
                   .eq(ApprovalFlowConfig::getStatus, 1)
                   .last("LIMIT 1");
            
            ApprovalFlowConfig config = flowConfigMapper.selectOne(wrapper);
            if (config == null) {
                return null;
            }
            
            // 解析流程定义
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ApprovalFlowDefinition flowDef = objectMapper.readValue(
                config.getFlowDefinition(), 
                ApprovalFlowDefinition.class
            );
            
            if (flowDef.getLevels() == null || flowDef.getLevels().size() < nextLevel) {
                return null;
            }
            
            // 获取下一级配置
            ApprovalFlowDefinition.ApprovalLevel nextLevelConfig = flowDef.getLevels().get(nextLevel - 1);
            
            // 使用审批流程引擎分配审批人
            return approvalWorkflowService.initializeApprovalWorkflow(
                context.getApplicationId(), 
                context.getApplicationNo(), 
                context
            );
        } catch (Exception e) {
            log.error("分配下一级审批人失败", e);
            return null;
        }
    }
    
    /**
     * 生成申请单号
     * 格式: APP + yyyyMMdd + 6位序号
     */
    private String generateApplicationNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int)(Math.random() * 1000000));
        return "APP" + dateStr + randomStr;
    }
}
