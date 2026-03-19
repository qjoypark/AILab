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
import com.lab.approval.service.ApproverAssignmentService;
import com.lab.approval.service.MaterialApplicationService;
import com.lab.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 棰嗙敤鐢宠鏈嶅姟瀹炵幇
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialApplicationServiceImpl implements MaterialApplicationService {

    private static final String ROLE_CENTER_ADMIN = "CENTER_ADMIN";

    private final MaterialApplicationMapper applicationMapper;
    private final MaterialApplicationItemMapper itemMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalFlowConfigMapper flowConfigMapper;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApproverAssignmentService approverAssignmentService;
    private final InventoryClient inventoryClient;
    private final MaterialClient materialClient;
    private final com.lab.approval.client.UserClient userClient;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApplication(CreateApplicationRequest request, Long applicantId, 
                                   String applicantName, String applicantDept) {
        log.info("鍒涘缓棰嗙敤鐢宠: applicantId={}, applicationType={}", applicantId, request.getApplicationType());
        
        // 1. 楠岃瘉鐢宠鏄庣粏
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("鐢宠鏄庣粏涓嶈兘涓虹┖");
        }
        
        // 2. 鍗卞寲鍝佺敵璇风壒娈婇獙璇?
        if (request.getApplicationType() != null && request.getApplicationType() == 2) {
            // 2.1 楠岃瘉鐢宠浜哄畨鍏ㄨ祫璐?
            boolean hasSafetyCert = userClient.checkSafetyCertification(applicantId);
            if (!hasSafetyCert) {
                throw new BusinessException("瀹夊叏璧勮川鏈€氳繃鎴栧凡杩囨湡锛屾棤娉曠敵璇峰嵄鍖栧搧");
            }
            
            // 2.2 验证必填字段：用途说明
            if (request.getUsagePurpose() == null || request.getUsagePurpose().trim().isEmpty()) {
                throw new BusinessException("危化品申请必须填写用途说明");
            }
            
            // 2.3 验证必填字段：使用地点
            if (request.getUsageLocation() == null || request.getUsageLocation().trim().isEmpty()) {
                throw new BusinessException("危化品申请必须填写使用地点");
            }
            
            log.info("鍗卞寲鍝佺敵璇烽獙璇侀€氳繃: applicantId={}, usagePurpose={}, usageLocation={}", 
                applicantId, request.getUsagePurpose(), request.getUsageLocation());
        }
        
        // 3. 妫€鏌ュ簱瀛樻槸鍚﹀厖瓒?
        for (CreateApplicationRequest.ApplicationItemRequest item : request.getItems()) {
            if (item.getApplyQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("鐢宠鏁伴噺蹇呴』澶т簬0");
            }

            MaterialInfo materialInfo = materialClient.getMaterialInfo(item.getMaterialId());
            if (materialInfo == null) {
                throw new BusinessException("鑽搧涓嶅瓨鍦紝ID: " + item.getMaterialId());
            }
            
            boolean stockAvailable = inventoryClient.checkStockAvailability(
                item.getMaterialId(), 
                item.getApplyQuantity()
            );
            
            if (!stockAvailable) {
                BigDecimal availableStock = inventoryClient.getAvailableStock(item.getMaterialId());
                throw new BusinessException(
                    String.format("鑽搧 %s 搴撳瓨涓嶈冻锛屽綋鍓嶅彲鐢ㄥ簱瀛? %s %s锛岀敵璇锋暟閲? %s %s",
                        materialInfo.getMaterialName(),
                        availableStock,
                        materialInfo.getUnit(),
                        item.getApplyQuantity(),
                        materialInfo.getUnit()
                    )
                );
            }
        }
        
        // 4. 鍒涘缓鐢宠鍗?
        MaterialApplication application = new MaterialApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setApplicantId(applicantId);
        application.setApplicantName(applicantName);
        application.setApplicantDept(applicantDept);
        application.setApplicationType(request.getApplicationType());
        application.setUsagePurpose(request.getUsagePurpose());
        application.setUsageLocation(request.getUsageLocation());
        application.setExpectedDate(request.getExpectedDate());
        application.setStatus(1); // 寰呭鎵?
        application.setApprovalStatus(0); // 鏈鎵?
        application.setRemark(request.getRemark());
        application.setCreatedBy(applicantId);
        application.setCreatedTime(LocalDateTime.now());
        
        applicationMapper.insert(application);
        
        // 5. 鍒涘缓鐢宠鏄庣粏
        for (CreateApplicationRequest.ApplicationItemRequest itemRequest : request.getItems()) {
            MaterialInfo materialInfo = materialClient.getMaterialInfo(itemRequest.getMaterialId());
            if (materialInfo == null) {
                throw new BusinessException("鑽搧涓嶅瓨鍦紝ID: " + itemRequest.getMaterialId());
            }
            
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
        
        // 6. 鍚姩瀹℃壒娴佺▼
        ApprovalContext context = new ApprovalContext();
        context.setBusinessType(request.getApplicationType()); // 1-普通领用, 2-危化品领用
        context.setApplicationType(request.getApplicationType());
        context.setApplicationId(application.getId());
        context.setApplicationNo(application.getApplicationNo());
        context.setApplicantId(applicantId);
        context.setApplicantDept(applicantDept);
        
        Long firstApproverId = approvalWorkflowService.initializeApprovalWorkflow(
            application.getId(),
            application.getApplicationNo(),
            context
        );
        
        // 7. 鏇存柊鐢宠鍗曠姸鎬佸拰褰撳墠瀹℃壒浜?
        application.setStatus(2); // 瀹℃壒涓?
        application.setApprovalStatus(1); // 瀹℃壒涓?
        application.setCurrentApproverId(firstApproverId);
        applicationMapper.updateById(application);
        
        log.info("棰嗙敤鐢宠鍒涘缓鎴愬姛: applicationId={}, applicationNo={}", 
            application.getId(), application.getApplicationNo());
        
        return application.getId();
    }
    
    @Override
    public Page<MaterialApplication> listApplications(
            int page,
            int size,
            Integer status,
            Integer applicationType,
            String keyword,
            Integer uiStatus,
            LocalDate startDate,
            LocalDate endDate) {
        log.info("鏌ヨ鐢宠鍒楄〃: page={}, size={}, status={}, applicationType={}", 
            page, size, status, applicationType);
        
        Page<MaterialApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MaterialApplication> wrapper = new LambdaQueryWrapper<>();
        
        if (uiStatus != null) {
            List<Integer> mappedStatuses = mapUiStatusToBackendStatuses(uiStatus);
            if (mappedStatuses.isEmpty()) {
                wrapper.eq(MaterialApplication::getStatus, -1);
            } else if (mappedStatuses.size() == 1) {
                wrapper.eq(MaterialApplication::getStatus, mappedStatuses.get(0));
            } else {
                wrapper.in(MaterialApplication::getStatus, mappedStatuses);
            }
        } else if (status != null) {
            wrapper.eq(MaterialApplication::getStatus, status);
        }
        if (applicationType != null) {
            wrapper.eq(MaterialApplication::getApplicationType, applicationType);
        }
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(condition -> condition
                    .like(MaterialApplication::getApplicationNo, trimmedKeyword)
                    .or()
                    .like(MaterialApplication::getApplicantName, trimmedKeyword)
                    .or()
                    .like(MaterialApplication::getApplicantDept, trimmedKeyword)
            );
        }
        if (startDate != null) {
            wrapper.ge(MaterialApplication::getCreatedTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(MaterialApplication::getCreatedTime, endDate.plusDays(1).atStartOfDay());
        }
        
        wrapper.orderByDesc(MaterialApplication::getCreatedTime);
        
        Page<MaterialApplication> resultPage = applicationMapper.selectPage(pageParam, wrapper);
        if (resultPage.getRecords() != null) {
            resultPage.getRecords().forEach(this::fillApplicationPendingInfo);
        }
        return resultPage;
    }

    private List<Integer> mapUiStatusToBackendStatuses(Integer uiStatus) {
        if (uiStatus == null) {
            return Collections.emptyList();
        }
        return switch (uiStatus) {
            case 1 -> Arrays.asList(1, 2);
            case 2 -> Collections.singletonList(3);
            case 3 -> Collections.singletonList(4);
            case 4 -> Arrays.asList(5, 6);
            case 5 -> Collections.singletonList(7);
            default -> Collections.emptyList();
        };
    }

    @Override
    public List<MaterialApplicationDTO> listPendingApplications(Long approverId) {
        if (approverId == null) {
            return List.of();
        }

        LambdaQueryWrapper<MaterialApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialApplication::getStatus, 2)
                .orderByDesc(MaterialApplication::getCreatedTime);

        List<MaterialApplication> applications = applicationMapper.selectList(wrapper).stream()
                .filter(application -> canUserApproveApplication(application, approverId))
                .collect(Collectors.toList());

        return applications.stream().map(application -> {
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
            fillApplicationPendingInfo(dto, application);
            dto.setCreatedTime(application.getCreatedTime());
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    public MaterialApplicationDTO getApplicationDetail(Long id) {
        log.info("鏌ヨ鐢宠璇︽儏: id={}", id);
        
        // 1. 鏌ヨ鐢宠鍗?
        MaterialApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("鐢宠鍗曚笉瀛樺湪");
        }
        
        // 2. 鏌ヨ鐢宠鏄庣粏
        LambdaQueryWrapper<MaterialApplicationItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MaterialApplicationItem::getApplicationId, id);
        List<MaterialApplicationItem> items = itemMapper.selectList(itemWrapper);
        
        // 3. 鏌ヨ瀹℃壒璁板綍
        List<ApprovalRecord> approvalRecords = approvalWorkflowService.getApprovalHistory(id);
        
        // 4. 缁勮DTO
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
        fillApplicationPendingInfo(dto, application);
        dto.setRemark(application.getRemark());
        dto.setCreatedTime(application.getCreatedTime());
        
        // 杞崲鐢宠鏄庣粏
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
        
        // 杞崲瀹℃壒璁板綍
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

        // 填充出库流程追踪信息（审批通过后用于显示“出库流程中”及出库单号）
        List<StockOutOrderInfoDTO> stockOutOrders = inventoryClient.getStockOutOrdersByApplicationId(id);
        dto.setStockOutOrders(stockOutOrders);
        if (stockOutOrders == null || stockOutOrders.isEmpty()) {
            dto.setStockOutFlowStatus(0);
            dto.setStockOutFlowStatusName("鏈敓鎴愬嚭搴撳崟");
            dto.setStockOutOrderNos("");
        } else {
            boolean allCompleted = stockOutOrders.stream()
                    .allMatch(order -> order.getStatus() != null && order.getStatus() == 2);
            dto.setStockOutFlowStatus(allCompleted ? 2 : 1);
            dto.setStockOutFlowStatusName(allCompleted ? "已全部出库" : "出库流程中");
            String orderNos = stockOutOrders.stream()
                    .map(StockOutOrderInfoDTO::getOutOrderNo)
                    .filter(orderNo -> orderNo != null && !orderNo.trim().isEmpty())
                    .collect(Collectors.joining(", "));
            dto.setStockOutOrderNos(orderNos);
        }
        
        return dto;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApplication(Long id, Long userId) {
        log.info("鍙栨秷鐢宠: id={}, userId={}", id, userId);
        
        MaterialApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("鐢宠鍗曚笉瀛樺湪");
        }
        
        // 鍙湁寰呭鎵瑰拰瀹℃壒涓殑鐢宠鍙互鍙栨秷
        if (application.getStatus() != 1 && application.getStatus() != 2) {
            throw new BusinessException("褰撳墠鐘舵€佷笉鍏佽鍙栨秷");
        }
        
        // 只有申请人可以取消
        if (!application.getApplicantId().equals(userId)) {
            throw new BusinessException("只有申请人可以取消申请");
        }
        
        application.setStatus(7); // 宸插彇娑?
        application.setUpdatedBy(userId);
        application.setUpdatedTime(LocalDateTime.now());
        
        applicationMapper.updateById(application);
        
        log.info("鐢宠宸插彇娑? id={}", id);
    }
    
    @Override
    public void updateApplicationStatus(Long id, Integer status) {
        log.info("鏇存柊鐢宠鐘舵€? id={}, status={}", id, status);
        
        MaterialApplication application = new MaterialApplication();
        application.setId(id);
        application.setStatus(status);
        application.setUpdatedTime(LocalDateTime.now());
        
        applicationMapper.updateById(application);
    }
    
    @Override
    public void updateApprovalStatus(Long id, Integer approvalStatus) {
        log.info("鏇存柊瀹℃壒鐘舵€? id={}, approvalStatus={}", id, approvalStatus);
        
        MaterialApplication application = new MaterialApplication();
        application.setId(id);
        application.setApprovalStatus(approvalStatus);
        application.setUpdatedTime(LocalDateTime.now());
        
        applicationMapper.updateById(application);
    }
    
    @Override
    public void updateCurrentApprover(Long id, Long approverId) {
        log.info("鏇存柊褰撳墠瀹℃壒浜? id={}, approverId={}", id, approverId);
        
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
        log.info("澶勭悊瀹℃壒: id={}, approverId={}, result={}", id, approverId, request.getApprovalResult());
        
        // 1. 鏌ヨ鐢宠鍗?
        MaterialApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("鐢宠鍗曚笉瀛樺湪");
        }
        
        // 2. 验证申请单状态
        if (application.getStatus() != 2) {
            throw new BusinessException("申请单不在审批中状态");
        }
        
        // 3. 鑾峰彇褰撳墠瀹℃壒灞傜骇
        List<ApprovalRecord> existingRecords = approvalWorkflowService.getApprovalHistory(id);
        int currentLevel = existingRecords.size() + 1;

        // 4. 鏍￠獙瀹℃壒鏉冮檺锛氭寜褰撳墠瀹℃壒瑙掕壊鍊欓€変汉鍖归厤锛堟敮鎸佸浜猴級
        ApprovalContext approvalContext = buildApprovalContext(application);
        String currentApproverRole = resolveApproverRoleByLevel(application.getApplicationType(), currentLevel, approvalContext);
        List<Long> candidateApproverIds = resolveApproverIds(currentApproverRole, approvalContext);
        boolean isCandidateApprover = candidateApproverIds.contains(approverId);
        boolean isPrimaryApprover = application.getCurrentApproverId() != null && application.getCurrentApproverId().equals(approverId);
        if (!isCandidateApprover && !isPrimaryApprover && !isAdminUser(approverId)) {
            throw new BusinessException(403001, "鎮ㄤ笉鏄綋鍓嶅鎵硅鑹茬殑鍙鎵逛汉锛屾棤鏉冨鎵硅鐢宠");
        }
        if (application.getCurrentApproverId() == null && candidateApproverIds.isEmpty()) {
            throw new BusinessException("褰撳墠鐢宠鍗曟湭鍒嗛厤瀹℃壒浜猴紝鏃犳硶瀹℃壒");
        }
        
        // 5. 濡傛灉瀹℃壒閫氳繃涓旀彁渚涗簡鎵瑰噯鏁伴噺锛屾洿鏂扮敵璇锋槑缁嗙殑鎵瑰噯鏁伴噺
        if (request.getApprovalResult() == 1 && request.getApprovedQuantities() != null) {
            for (com.lab.approval.dto.ApprovalProcessRequest.ApprovedQuantityItem item : request.getApprovedQuantities()) {
                MaterialApplicationItem applicationItem = itemMapper.selectById(item.getItemId());
                if (applicationItem == null || !applicationItem.getApplicationId().equals(id)) {
                    throw new BusinessException("申请明细不存在或不属于该申请单");
                }
                
                // 楠岃瘉鎵瑰噯鏁伴噺涓嶈兘瓒呰繃鐢宠鏁伴噺
                if (item.getApprovedQuantity().compareTo(applicationItem.getApplyQuantity()) > 0) {
                    throw new BusinessException(
                        String.format("药品 %s 的批准数量不能超过申请数量", applicationItem.getMaterialName())
                    );
                }
                
                // 楠岃瘉鎵瑰噯鏁伴噺涓嶈兘涓鸿礋鏁?
                if (item.getApprovedQuantity().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException("批准数量不能为负数");
                }
                
                // 鏇存柊鎵瑰噯鏁伴噺
                applicationItem.setApprovedQuantity(item.getApprovedQuantity());
                itemMapper.updateById(applicationItem);
            }
        }
        
        // 6. 璁板綍瀹℃壒鎰忚鍒癮pproval_record琛?
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
        
        // 7. 鏍规嵁瀹℃壒缁撴灉澶勭悊娴佽浆
        if (request.getApprovalResult() == 1) {
            // 瀹℃壒閫氳繃锛屾鏌ユ槸鍚﹁繕鏈変笅涓€绾?
            handleApprovalPass(application, currentLevel, approverId);
        } else if (request.getApprovalResult() == 2) {
            // 瀹℃壒鎷掔粷锛岀粓姝㈡祦绋?
            handleApprovalReject(application);
        } else {
            throw new BusinessException("无效的审批结果");
        }
        
        log.info("瀹℃壒澶勭悊瀹屾垚: id={}, result={}", id, request.getApprovalResult());
    }
    
    /**
     * 澶勭悊瀹℃壒閫氳繃
     */
    private void handleApprovalPass(MaterialApplication application, int currentLevel, Long approverId) {
        log.info("澶勭悊瀹℃壒閫氳繃: applicationId={}, currentLevel={}", application.getId(), currentLevel);
        
        // 获取审批上下文
        ApprovalContext context = buildApprovalContext(application);
        
        // 妫€鏌ユ槸鍚﹁繕鏈変笅涓€绾у鎵?
        boolean hasNextLevel = checkHasNextLevel(application.getApplicationType(), currentLevel);
        
        if (hasNextLevel) {
            // 娴佽浆鍒颁笅涓€绾?
            Long nextApproverId = assignNextLevelApprover(application.getApplicationType(), currentLevel + 1, context);
            
            if (nextApproverId == null) {
                throw new BusinessException("鏃犳硶鍒嗛厤涓嬩竴绾у鎵逛汉");
            }
            
            application.setCurrentApproverId(nextApproverId);
            application.setApprovalStatus(1); // 瀹℃壒涓?
            application.setStatus(2); // 瀹℃壒涓?
            application.setUpdatedTime(LocalDateTime.now());
            applicationMapper.updateById(application);
            
            // TODO: 鍙戦€佸鎵归€氱煡缁欎笅涓€绾у鎵逛汉
            log.info("娴佽浆鍒颁笅涓€绾у鎵? applicationId={}, nextLevel={}, nextApproverId={}", 
                application.getId(), currentLevel + 1, nextApproverId);
        } else {
            // 鎵€鏈夊鎵归€氳繃锛屾爣璁颁负瀹℃壒瀹屾垚
            application.setApprovalStatus(2); // 瀹℃壒閫氳繃
            application.setStatus(3); // 瀹℃壒閫氳繃
            application.setCurrentApproverId(null);
            application.setUpdatedTime(LocalDateTime.now());
            applicationMapper.updateById(application);

            triggerAutoCreateStockOutOrder(application.getId());

            // TODO: 审批通过后通知申请人
            log.info("瀹℃壒娴佺▼瀹屾垚: applicationId={}", application.getId());
        }
    }
    
    /**
     * 澶勭悊瀹℃壒鎷掔粷
     */
    private void handleApprovalReject(MaterialApplication application) {
        log.info("澶勭悊瀹℃壒鎷掔粷: applicationId={}", application.getId());
        
        application.setApprovalStatus(3); // 瀹℃壒鎷掔粷
        application.setStatus(4); // 瀹℃壒鎷掔粷
        application.setCurrentApproverId(null);
        application.setUpdatedTime(LocalDateTime.now());
        applicationMapper.updateById(application);
        
        // TODO: 鍙戦€佸鎵规嫆缁濋€氱煡缁欑敵璇蜂汉
        log.info("瀹℃壒宸叉嫆缁? applicationId={}", application.getId());
    }
    
    /**
     * 妫€鏌ユ槸鍚﹁繕鏈変笅涓€绾у鎵?
     */
    private boolean checkHasNextLevel(Integer businessType, int currentLevel) {
        if (isCenterAdminSingleLevelPolicy(businessType)) {
            return false;
        }
        try {
            // 鑾峰彇娴佺▼閰嶇疆
            LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
                   .eq(ApprovalFlowConfig::getStatus, 1)
                   .last("LIMIT 1");
            
            ApprovalFlowConfig config = flowConfigMapper.selectOne(wrapper);
            if (config == null) {
                // 濡傛灉娌℃湁閰嶇疆锛岄粯璁ゅ彧鏈変竴绾у鎵?
                return false;
            }
            
            // 瑙ｆ瀽娴佺▼瀹氫箟
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ApprovalFlowDefinition flowDef = objectMapper.readValue(
                config.getFlowDefinition(), 
                ApprovalFlowDefinition.class
            );
            
            // 妫€鏌ユ槸鍚﹁繕鏈変笅涓€绾?
            return flowDef.getLevels() != null && flowDef.getLevels().size() > currentLevel;
        } catch (Exception e) {
            log.error("检查下一级审批失败", e);
            // 鍑洪敊鏃堕粯璁ゆ病鏈変笅涓€绾?
            return false;
        }
    }
    
    /**
     * 鍒嗛厤涓嬩竴绾у鎵逛汉
     */
    private Long assignNextLevelApprover(Integer businessType, int nextLevel, ApprovalContext context) {
        try {
            // 鑾峰彇娴佺▼閰嶇疆
            LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
                   .eq(ApprovalFlowConfig::getStatus, 1)
                   .last("LIMIT 1");
            
            ApprovalFlowConfig config = flowConfigMapper.selectOne(wrapper);
            if (config == null) {
                return null;
            }
            
            // 瑙ｆ瀽娴佺▼瀹氫箟
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

            // 鎸夎鑹插姩鎬佸垎閰嶄笅涓€绾у鎵逛汉锛堝€欓€変汉涓殑绗竴涓級
            return approverAssignmentService.assignApprover(nextLevelConfig.getApproverRole(), context);
        } catch (Exception e) {
            log.error("鍒嗛厤涓嬩竴绾у鎵逛汉澶辫触", e);
            return null;
        }
    }
    
    /**
     * 鐢熸垚鐢宠鍗曞彿
     * 鏍煎紡: APP + yyyyMMdd + 6浣嶅簭鍙?
     */
    private String generateApplicationNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int)(Math.random() * 1000000));
        return "APP" + dateStr + randomStr;
    }

    private void fillApplicationPendingInfo(MaterialApplication application) {
        if (application == null) {
            return;
        }
        List<ApproverCandidateDTO> candidates = resolveCurrentApproverCandidates(application);
        application.setCurrentApproverRole(resolveCurrentApproverRole(application));
        application.setCurrentApproverIds(candidates.stream().map(ApproverCandidateDTO::getUserId).collect(Collectors.toList()));
        application.setCurrentApproverNames(candidates.stream().map(ApproverCandidateDTO::getDisplayName).collect(Collectors.toList()));
        String primaryApproverName = candidates.isEmpty() ? "" : candidates.get(0).getDisplayName();
        application.setCurrentApproverName(primaryApproverName);
        application.setCurrentPendingStatus(resolvePendingStatusStrict(
                application.getStatus(),
                application.getApprovalStatus(),
                application.getCurrentApproverIds()
        ));
    }

    private void fillApplicationPendingInfo(MaterialApplicationDTO dto, MaterialApplication application) {
        if (dto == null || application == null) {
            return;
        }
        List<ApproverCandidateDTO> candidates = resolveCurrentApproverCandidates(application);
        dto.setCurrentApproverRole(resolveCurrentApproverRole(application));
        dto.setCurrentApproverIds(candidates.stream().map(ApproverCandidateDTO::getUserId).collect(Collectors.toList()));
        dto.setCurrentApproverNames(candidates.stream().map(ApproverCandidateDTO::getDisplayName).collect(Collectors.toList()));
        String primaryApproverName = candidates.isEmpty() ? "" : candidates.get(0).getDisplayName();
        dto.setCurrentApproverName(primaryApproverName);
        dto.setCurrentPendingStatus(resolvePendingStatusStrict(
                application.getStatus(),
                application.getApprovalStatus(),
                dto.getCurrentApproverIds()
        ));
    }

    private boolean canUserApproveApplication(MaterialApplication application, Long approverId) {
        if (application == null || approverId == null || application.getStatus() == null || application.getStatus() != 2) {
            return false;
        }
        if (isAdminUser(approverId)) {
            return true;
        }
        List<Long> candidateIds = resolveCurrentApproverCandidates(application).stream()
                .map(ApproverCandidateDTO::getUserId)
                .collect(Collectors.toList());
        return candidateIds.contains(approverId) || Objects.equals(application.getCurrentApproverId(), approverId);
    }

    private List<ApproverCandidateDTO> resolveCurrentApproverCandidates(MaterialApplication application) {
        if (application == null) {
            return Collections.emptyList();
        }
        String approverRole = resolveCurrentApproverRole(application);
        List<ApproverCandidateDTO> candidates = resolveApprovers(approverRole, buildApprovalContext(application));
        return ensureCurrentApproverPresent(candidates, application.getCurrentApproverId(), approverRole);
    }

    private String resolveCurrentApproverRole(MaterialApplication application) {
        if (application == null || application.getId() == null) {
            return null;
        }
        if (application.getStatus() == null || (application.getStatus() != 1 && application.getStatus() != 2)) {
            return null;
        }
        List<ApprovalRecord> records = approvalWorkflowService.getApprovalHistory(application.getId());
        int currentLevel = records.size() + 1;
        return resolveApproverRoleByLevel(
                application.getApplicationType(),
                currentLevel,
                buildApprovalContext(application)
        );
    }

    private String resolveApproverRoleByLevel(Integer businessType, int level, ApprovalContext context) {
        if (isCenterAdminSingleLevelPolicy(businessType)) {
            return level == 1 ? ROLE_CENTER_ADMIN : null;
        }

        ApprovalFlowDefinition flowDefinition = resolveFlowDefinition(businessType);
        if (flowDefinition == null || flowDefinition.getLevels() == null || flowDefinition.getLevels().size() < level) {
            return null;
        }
        ApprovalFlowDefinition.ApprovalLevel currentLevelConfig = flowDefinition.getLevels().get(level - 1);
        if (currentLevelConfig == null) {
            return null;
        }
        return approverAssignmentService.resolveEffectiveRoleCode(currentLevelConfig.getApproverRole(), context);
    }

    private ApprovalFlowDefinition resolveFlowDefinition(Integer businessType) {
        try {
            LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApprovalFlowConfig::getBusinessType, businessType)
                    .eq(ApprovalFlowConfig::getStatus, 1)
                    .last("LIMIT 1");
            ApprovalFlowConfig config = flowConfigMapper.selectOne(wrapper);
            if (config == null || config.getFlowDefinition() == null || config.getFlowDefinition().isEmpty()) {
                return null;
            }
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(config.getFlowDefinition(), ApprovalFlowDefinition.class);
        } catch (Exception exception) {
            log.warn("瑙ｆ瀽瀹℃壒娴佺▼瀹氫箟澶辫触: businessType={}", businessType, exception);
            return null;
        }
    }

    private ApprovalContext buildApprovalContext(MaterialApplication application) {
        ApprovalContext context = new ApprovalContext();
        if (application != null) {
            context.setBusinessType(application.getApplicationType());
            context.setApplicationType(application.getApplicationType());
            context.setApplicationId(application.getId());
            context.setApplicationNo(application.getApplicationNo());
            context.setApplicantId(application.getApplicantId());
            context.setApplicantDept(application.getApplicantDept());
        }
        return context;
    }

    private List<ApproverCandidateDTO> resolveApprovers(String approverRole, ApprovalContext context) {
        if (approverRole == null || approverRole.isBlank()) {
            return Collections.emptyList();
        }
        List<ApproverCandidateDTO> candidates = approverAssignmentService.listApproverCandidates(approverRole, context);
        return candidates == null ? Collections.emptyList() : candidates;
    }

    private List<Long> resolveApproverIds(String approverRole, ApprovalContext context) {
        List<ApproverCandidateDTO> candidates = resolveApprovers(approverRole, context);
        return candidates.stream().map(ApproverCandidateDTO::getUserId).collect(Collectors.toList());
    }

    private List<ApproverCandidateDTO> ensureCurrentApproverPresent(List<ApproverCandidateDTO> candidates,
                                                                    Long currentApproverId,
                                                                    String approverRole) {
        List<ApproverCandidateDTO> safeCandidates = new ArrayList<>(
                candidates == null ? Collections.emptyList() : candidates
        );
        if (currentApproverId == null) {
            return safeCandidates;
        }
        boolean exists = safeCandidates.stream()
                .anyMatch(candidate -> Objects.equals(candidate.getUserId(), currentApproverId));
        if (exists) {
            return safeCandidates;
        }

        ApproverCandidateDTO fallback = new ApproverCandidateDTO();
        fallback.setUserId(currentApproverId);
        fallback.setRoleCode(approverRole == null ? ROLE_CENTER_ADMIN : approverRole);
        try {
            UserInfo userInfo = userClient.getUserInfo(currentApproverId);
            if (userInfo != null) {
                fallback.setUsername(userInfo.getUsername());
                fallback.setRealName(userInfo.getRealName());
            }
        } catch (Exception exception) {
            log.warn("鍥炲～褰撳墠瀹℃壒浜哄悕绉板け璐? approverId={}", currentApproverId, exception);
        }
        safeCandidates.add(0, fallback);
        return safeCandidates;
    }

    private String resolvePendingStatus(Integer status, Integer approvalStatus, List<Long> currentApproverIds) {
        if (status == null) {
            return "待处理";
        }
        if (status == 1 || status == 2) {
            if (currentApproverIds == null || currentApproverIds.isEmpty()) {
                return "待分配审批人";
            }
            return "待审批";
        }
        if (status == 3 || (approvalStatus != null && approvalStatus == 2)) {
            return "审批通过";
        }
        if (status == 4 || (approvalStatus != null && approvalStatus == 3)) {
            return "审批拒绝";
        }
        if (status == 5) {
            return "出库流程中";
        }
        if (status == 6) {
            return "已完成";
        }
        if (status == 7) {
            return "已取消";
        }
        return "待处理";
    }

    /**
     * 当前临时审批策略：
     * 普通领用、危化领用统一由实验中心主任（CENTER_ADMIN）审批，按单级流程处理。
     */
    private String resolvePendingStatusStrict(Integer status, Integer approvalStatus, List<Long> currentApproverIds) {
        if (status == null) {
            return "待处理";
        }
        if (status == 1 || status == 2) {
            return "待审批";
        }
        if (status == 3 || (approvalStatus != null && approvalStatus == 2)) {
            return "审批通过";
        }
        if (status == 4 || (approvalStatus != null && approvalStatus == 3)) {
            return "审批拒绝";
        }
        if (status == 5) {
            return "出库流程中";
        }
        if (status == 6) {
            return "已完成";
        }
        if (status == 7) {
            return "已取消";
        }
        return "待处理";
    }

    private boolean isCenterAdminSingleLevelPolicy(Integer businessType) {
        return businessType != null && (businessType == 1 || businessType == 2);
    }

    private void triggerAutoCreateStockOutOrder(Long applicationId) {
        if (applicationId == null) {
            return;
        }

        Runnable createTask = () -> {
            try {
                Long stockOutId = inventoryClient.createStockOutOrder(applicationId);
                if (stockOutId != null) {
                    log.info("审批通过后自动创建出库单成功: applicationId={}, stockOutId={}", applicationId, stockOutId);
                } else {
                    log.warn("审批通过后自动创建出库单失败: applicationId={}", applicationId);
                }
            } catch (Exception exception) {
                log.error("审批通过后自动创建出库单异常: applicationId={}", applicationId, exception);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    createTask.run();
                }
            });
            return;
        }

        createTask.run();
    }
    private boolean isAdminUser(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            com.lab.approval.dto.UserInfo userInfo = userClient.getUserInfo(userId);
            if (userInfo == null) {
                return false;
            }
            if (userInfo.getUserType() != null && userInfo.getUserType() == 1) {
                return true;
            }
            String username = userInfo.getUsername();
            return username != null && "admin".equalsIgnoreCase(username.trim());
        } catch (Exception exception) {
            log.warn("鍒ゆ柇绠＄悊鍛樿韩浠藉け璐? userId={}", userId, exception);
            return false;
        }
    }
}
