package com.lab.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.approval.client.InventoryClient;
import com.lab.approval.client.MaterialClient;
import com.lab.approval.dto.ApprovalProcessRequest;
import com.lab.approval.dto.MaterialInfo;
import com.lab.approval.entity.ApprovalFlowConfig;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.entity.MaterialApplicationItem;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.mapper.MaterialApplicationItemMapper;
import com.lab.approval.mapper.MaterialApplicationMapper;
import com.lab.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 审批处理测试
 */
@SpringBootTest
@Transactional
@DisplayName("审批处理功能测试")
class ApprovalProcessingTest {
    
    @Autowired
    private MaterialApplicationService applicationService;
    
    @Autowired
    private MaterialApplicationMapper applicationMapper;
    
    @Autowired
    private MaterialApplicationItemMapper itemMapper;
    
    @Autowired
    private ApprovalRecordMapper recordMapper;
    
    @Autowired
    private ApprovalFlowConfigMapper flowConfigMapper;
    
    @MockBean
    private InventoryClient inventoryClient;
    
    @MockBean
    private MaterialClient materialClient;
    
    private Long testApplicationId;
    private Long testItemId;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        setupTestData();
    }
    
    private void setupTestData() {
        // 创建测试申请单
        MaterialApplication application = new MaterialApplication();
        application.setApplicationNo("APP20240101000001");
        application.setApplicantId(1L);
        application.setApplicantName("测试申请人");
        application.setApplicantDept("测试部门");
        application.setApplicationType(1); // 普通领用
        application.setUsagePurpose("测试用途");
        application.setUsageLocation("测试地点");
        application.setExpectedDate(LocalDate.now().plusDays(7));
        application.setStatus(2); // 审批中
        application.setApprovalStatus(1); // 审批中
        application.setCurrentApproverId(2L); // 当前审批人
        application.setCreatedBy(1L);
        application.setCreatedTime(LocalDateTime.now());
        applicationMapper.insert(application);
        testApplicationId = application.getId();
        
        // 创建测试申请明细
        MaterialApplicationItem item = new MaterialApplicationItem();
        item.setApplicationId(testApplicationId);
        item.setMaterialId(1L);
        item.setMaterialName("测试药品");
        item.setSpecification("100ml");
        item.setUnit("瓶");
        item.setApplyQuantity(new BigDecimal("10"));
        item.setCreatedTime(LocalDateTime.now());
        itemMapper.insert(item);
        testItemId = item.getId();
        
        // 创建流程配置
        ApprovalFlowConfig flowConfig = new ApprovalFlowConfig();
        flowConfig.setFlowCode("NORMAL_APPROVAL");
        flowConfig.setFlowName("普通领用审批");
        flowConfig.setBusinessType(1);
        flowConfig.setFlowDefinition("{\"levels\":[{\"level\":1,\"approverRole\":\"LAB_MANAGER\"}]}");
        flowConfig.setStatus(1);
        flowConfig.setCreatedTime(LocalDateTime.now());
        flowConfigMapper.insert(flowConfig);
        
        // Mock材料信息
        MaterialInfo materialInfo = new MaterialInfo();
        materialInfo.setMaterialId(1L);
        materialInfo.setMaterialName("测试药品");
        materialInfo.setSpecification("100ml");
        materialInfo.setUnit("瓶");
        when(materialClient.getMaterialInfo(anyLong())).thenReturn(materialInfo);
    }
    
    @Test
    @DisplayName("审批通过 - 单级审批")
    void testApprovalPass_SingleLevel() {
        // 准备审批请求
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1); // 通过
        request.setApprovalOpinion("同意");
        
        // 执行审批
        applicationService.processApproval(testApplicationId, request, 2L, "审批人");
        
        // 验证申请单状态
        MaterialApplication application = applicationMapper.selectById(testApplicationId);
        assertNotNull(application);
        assertEquals(3, application.getStatus()); // 审批通过
        assertEquals(2, application.getApprovalStatus()); // 审批通过
        assertNull(application.getCurrentApproverId()); // 审批完成，无当前审批人
        
        // 验证审批记录
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getApplicationId, testApplicationId);
        List<ApprovalRecord> records = recordMapper.selectList(wrapper);
        assertEquals(1, records.size());
        
        ApprovalRecord record = records.get(0);
        assertEquals(2L, record.getApproverId());
        assertEquals("审批人", record.getApproverName());
        assertEquals(1, record.getApprovalLevel());
        assertEquals(1, record.getApprovalResult());
        assertEquals("同意", record.getApprovalOpinion());
    }
    
    @Test
    @DisplayName("审批通过 - 修改批准数量")
    void testApprovalPass_WithModifiedQuantity() {
        // 准备审批请求，修改批准数量
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1); // 通过
        request.setApprovalOpinion("同意，但减少数量");
        
        List<ApprovalProcessRequest.ApprovedQuantityItem> quantities = new ArrayList<>();
        ApprovalProcessRequest.ApprovedQuantityItem item = new ApprovalProcessRequest.ApprovedQuantityItem();
        item.setItemId(testItemId);
        item.setApprovedQuantity(new BigDecimal("8")); // 批准8瓶，申请10瓶
        quantities.add(item);
        request.setApprovedQuantities(quantities);
        
        // 执行审批
        applicationService.processApproval(testApplicationId, request, 2L, "审批人");
        
        // 验证批准数量已更新
        MaterialApplicationItem updatedItem = itemMapper.selectById(testItemId);
        assertNotNull(updatedItem);
        assertEquals(new BigDecimal("8"), updatedItem.getApprovedQuantity());
    }
    
    @Test
    @DisplayName("审批拒绝")
    void testApprovalReject() {
        // 准备审批请求
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(2); // 拒绝
        request.setApprovalOpinion("不符合要求");
        
        // 执行审批
        applicationService.processApproval(testApplicationId, request, 2L, "审批人");
        
        // 验证申请单状态
        MaterialApplication application = applicationMapper.selectById(testApplicationId);
        assertNotNull(application);
        assertEquals(4, application.getStatus()); // 审批拒绝
        assertEquals(3, application.getApprovalStatus()); // 审批拒绝
        assertNull(application.getCurrentApproverId()); // 审批终止，无当前审批人
        
        // 验证审批记录
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getApplicationId, testApplicationId);
        List<ApprovalRecord> records = recordMapper.selectList(wrapper);
        assertEquals(1, records.size());
        
        ApprovalRecord record = records.get(0);
        assertEquals(2, record.getApprovalResult()); // 拒绝
        assertEquals("不符合要求", record.getApprovalOpinion());
    }
    
    @Test
    @DisplayName("审批失败 - 非当前审批人")
    void testApprovalFail_NotCurrentApprover() {
        // 准备审批请求
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1);
        request.setApprovalOpinion("同意");
        
        // 使用错误的审批人ID
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.processApproval(testApplicationId, request, 999L, "错误审批人");
        });
        
        assertTrue(exception.getMessage().contains("不是当前审批人"));
    }
    
    @Test
    @DisplayName("审批失败 - 申请单不在审批中")
    void testApprovalFail_NotInApprovalStatus() {
        // 修改申请单状态为已完成
        MaterialApplication application = applicationMapper.selectById(testApplicationId);
        application.setStatus(6); // 已完成
        applicationMapper.updateById(application);
        
        // 准备审批请求
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1);
        request.setApprovalOpinion("同意");
        
        // 执行审批应该失败
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.processApproval(testApplicationId, request, 2L, "审批人");
        });
        
        assertTrue(exception.getMessage().contains("不在审批中状态"));
    }
    
    @Test
    @DisplayName("审批失败 - 批准数量超过申请数量")
    void testApprovalFail_ApprovedQuantityExceedsApplyQuantity() {
        // 准备审批请求，批准数量超过申请数量
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1);
        request.setApprovalOpinion("同意");
        
        List<ApprovalProcessRequest.ApprovedQuantityItem> quantities = new ArrayList<>();
        ApprovalProcessRequest.ApprovedQuantityItem item = new ApprovalProcessRequest.ApprovedQuantityItem();
        item.setItemId(testItemId);
        item.setApprovedQuantity(new BigDecimal("15")); // 批准15瓶，申请10瓶
        quantities.add(item);
        request.setApprovedQuantities(quantities);
        
        // 执行审批应该失败
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.processApproval(testApplicationId, request, 2L, "审批人");
        });
        
        assertTrue(exception.getMessage().contains("不能超过申请数量"));
    }
    
    @Test
    @DisplayName("审批失败 - 批准数量为负数")
    void testApprovalFail_NegativeApprovedQuantity() {
        // 准备审批请求，批准数量为负数
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1);
        request.setApprovalOpinion("同意");
        
        List<ApprovalProcessRequest.ApprovedQuantityItem> quantities = new ArrayList<>();
        ApprovalProcessRequest.ApprovedQuantityItem item = new ApprovalProcessRequest.ApprovedQuantityItem();
        item.setItemId(testItemId);
        item.setApprovedQuantity(new BigDecimal("-5")); // 负数
        quantities.add(item);
        request.setApprovedQuantities(quantities);
        
        // 执行审批应该失败
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.processApproval(testApplicationId, request, 2L, "审批人");
        });
        
        assertTrue(exception.getMessage().contains("不能为负数"));
    }
    
    @Test
    @DisplayName("审批记录完整性")
    void testApprovalRecordCompleteness() {
        // 准备审批请求
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1);
        request.setApprovalOpinion("审批意见测试");
        
        // 执行审批
        applicationService.processApproval(testApplicationId, request, 2L, "测试审批人");
        
        // 验证审批记录的完整性
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getApplicationId, testApplicationId);
        List<ApprovalRecord> records = recordMapper.selectList(wrapper);
        
        assertEquals(1, records.size());
        ApprovalRecord record = records.get(0);
        
        // 验证所有必需字段
        assertNotNull(record.getId());
        assertEquals(testApplicationId, record.getApplicationId());
        assertEquals("APP20240101000001", record.getApplicationNo());
        assertEquals(2L, record.getApproverId());
        assertEquals("测试审批人", record.getApproverName());
        assertEquals(1, record.getApprovalLevel());
        assertEquals(1, record.getApprovalResult());
        assertEquals("审批意见测试", record.getApprovalOpinion());
        assertNotNull(record.getApprovalTime());
        assertNotNull(record.getCreatedTime());
    }
}
