package com.lab.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.approval.client.InventoryClient;
import com.lab.approval.client.MaterialClient;
import com.lab.approval.dto.ApprovalProcessRequest;
import com.lab.approval.dto.CreateApplicationRequest;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 申请审批集成测试
 * 
 * 测试范围：
 * 1. 审批流程流转逻辑
 * 2. 库存检查
 * 3. 权限控制
 * 
 * 验证需求: 6.3, 6.4
 */
@SpringBootTest
@Transactional
@DisplayName("申请审批集成测试 - 需求6.3, 6.4")
@org.springframework.test.context.ActiveProfiles("test")
class ApprovalWorkflowIntegrationTest {
    
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
    
    private Long testUserId = 1L;
    private String testUserName = "测试用户";
    private String testDept = "测试部门";
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        cleanupTestData();
        
        // 准备流程配置
        setupFlowConfigs();
        
        // Mock材料信息
        setupMaterialMocks();
    }
    
    private void cleanupTestData() {
        // 清理可能存在的测试数据
        applicationMapper.delete(new LambdaQueryWrapper<>());
        itemMapper.delete(new LambdaQueryWrapper<>());
        recordMapper.delete(new LambdaQueryWrapper<>());
        flowConfigMapper.delete(new LambdaQueryWrapper<>());
    }
    
    private void setupFlowConfigs() {
        // 普通领用审批流程（单级）
        ApprovalFlowConfig normalFlow = new ApprovalFlowConfig();
        normalFlow.setFlowCode("NORMAL_APPROVAL");
        normalFlow.setFlowName("普通领用审批");
        normalFlow.setBusinessType(1);
        normalFlow.setFlowDefinition("{\"levels\":[{\"level\":1,\"approverRole\":\"LAB_MANAGER\",\"approverName\":\"实验室负责人\"}]}");
        normalFlow.setStatus(1);
        normalFlow.setCreatedTime(LocalDateTime.now());
        flowConfigMapper.insert(normalFlow);
        
        // 危化品领用审批流程（多级）
        ApprovalFlowConfig hazardousFlow = new ApprovalFlowConfig();
        hazardousFlow.setFlowCode("HAZARDOUS_APPROVAL");
        hazardousFlow.setFlowName("危化品领用审批");
        hazardousFlow.setBusinessType(2);
        hazardousFlow.setFlowDefinition("{\"levels\":[" +
                "{\"level\":1,\"approverRole\":\"LAB_MANAGER\",\"approverName\":\"实验室负责人\"}," +
                "{\"level\":2,\"approverRole\":\"CENTER_ADMIN\",\"approverName\":\"中心管理员\"}," +
                "{\"level\":3,\"approverRole\":\"ADMIN\",\"approverName\":\"安全管理员\"}" +
                "]}");
        hazardousFlow.setStatus(1);
        hazardousFlow.setCreatedTime(LocalDateTime.now());
        flowConfigMapper.insert(hazardousFlow);
    }
    
    private void setupMaterialMocks() {
        // Mock普通材料
        MaterialInfo normalMaterial = new MaterialInfo();
        normalMaterial.setMaterialId(1L);
        normalMaterial.setMaterialName("普通试剂");
        normalMaterial.setSpecification("100ml");
        normalMaterial.setUnit("瓶");
        normalMaterial.setMaterialType(2); // 试剂
        normalMaterial.setIsControlled(0); // 非管控
        
        // Mock危化品
        MaterialInfo hazardousMaterial = new MaterialInfo();
        hazardousMaterial.setMaterialId(2L);
        hazardousMaterial.setMaterialName("危险化学品");
        hazardousMaterial.setSpecification("500ml");
        hazardousMaterial.setUnit("瓶");
        hazardousMaterial.setMaterialType(3); // 危化品
        hazardousMaterial.setIsControlled(1); // 易制毒
        
        when(materialClient.getMaterialInfo(1L)).thenReturn(normalMaterial);
        when(materialClient.getMaterialInfo(2L)).thenReturn(hazardousMaterial);
    }
    
    // ==================== 审批流程流转逻辑测试 ====================
    
    @Test
    @DisplayName("单级审批流程 - 审批通过后状态正确更新")
    void testSingleLevelApproval_StatusUpdatedCorrectly() {
        // 1. 创建申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 验证初始状态
        MaterialApplication application = applicationMapper.selectById(applicationId);
        assertEquals(2, application.getStatus()); // 审批中
        assertEquals(1, application.getApprovalStatus()); // 审批中
        assertNotNull(application.getCurrentApproverId());
        
        // 2. 执行审批通过
        Long approverId = application.getCurrentApproverId();
        ApprovalProcessRequest approvalRequest = new ApprovalProcessRequest();
        approvalRequest.setApprovalResult(1); // 通过
        approvalRequest.setApprovalOpinion("同意");
        
        applicationService.processApproval(applicationId, approvalRequest, approverId, "审批人");
        
        // 3. 验证审批后状态
        application = applicationMapper.selectById(applicationId);
        assertEquals(3, application.getStatus()); // 审批通过
        assertEquals(2, application.getApprovalStatus()); // 审批通过
        assertNull(application.getCurrentApproverId()); // 审批完成，无当前审批人
    }
    
    @Test
    @DisplayName("单级审批流程 - 审批拒绝后状态正确更新")
    void testSingleLevelApproval_RejectedStatusUpdatedCorrectly() {
        // 1. 创建申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 2. 执行审批拒绝
        MaterialApplication application = applicationMapper.selectById(applicationId);
        Long approverId = application.getCurrentApproverId();
        
        ApprovalProcessRequest approvalRequest = new ApprovalProcessRequest();
        approvalRequest.setApprovalResult(2); // 拒绝
        approvalRequest.setApprovalOpinion("不符合要求");
        
        applicationService.processApproval(applicationId, approvalRequest, approverId, "审批人");
        
        // 3. 验证审批后状态
        application = applicationMapper.selectById(applicationId);
        assertEquals(4, application.getStatus()); // 审批拒绝
        assertEquals(3, application.getApprovalStatus()); // 审批拒绝
        assertNull(application.getCurrentApproverId()); // 审批终止
    }
    
    @Test
    @DisplayName("多级审批流程 - 第一级通过后流转到第二级")
    void testMultiLevelApproval_FlowsToNextLevel() {
        // 1. 创建危化品申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createHazardousApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 验证初始状态
        MaterialApplication application = applicationMapper.selectById(applicationId);
        assertEquals(2, application.getStatus()); // 审批中
        Long firstApproverId = application.getCurrentApproverId();
        assertNotNull(firstApproverId);
        
        // 2. 第一级审批通过
        ApprovalProcessRequest approvalRequest = new ApprovalProcessRequest();
        approvalRequest.setApprovalResult(1); // 通过
        approvalRequest.setApprovalOpinion("第一级同意");
        
        applicationService.processApproval(applicationId, approvalRequest, firstApproverId, "第一级审批人");
        
        // 3. 验证流转到第二级
        application = applicationMapper.selectById(applicationId);
        assertEquals(2, application.getStatus()); // 仍在审批中
        assertEquals(1, application.getApprovalStatus()); // 审批中
        assertNotNull(application.getCurrentApproverId()); // 有新的审批人
        assertNotEquals(firstApproverId, application.getCurrentApproverId()); // 审批人已变更
        
        // 4. 验证审批记录
        List<ApprovalRecord> records = getApprovalRecords(applicationId);
        assertEquals(1, records.size());
        assertEquals(1, records.get(0).getApprovalLevel());
        assertEquals(1, records.get(0).getApprovalResult());
    }
    
    @Test
    @DisplayName("多级审批流程 - 中间级拒绝后流程终止")
    void testMultiLevelApproval_TerminatesOnRejection() {
        // 1. 创建危化品申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createHazardousApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 2. 第一级审批通过
        MaterialApplication application = applicationMapper.selectById(applicationId);
        Long firstApproverId = application.getCurrentApproverId();
        
        ApprovalProcessRequest firstApproval = new ApprovalProcessRequest();
        firstApproval.setApprovalResult(1);
        firstApproval.setApprovalOpinion("第一级同意");
        applicationService.processApproval(applicationId, firstApproval, firstApproverId, "第一级审批人");
        
        // 3. 第二级审批拒绝
        application = applicationMapper.selectById(applicationId);
        Long secondApproverId = application.getCurrentApproverId();
        
        ApprovalProcessRequest secondApproval = new ApprovalProcessRequest();
        secondApproval.setApprovalResult(2); // 拒绝
        secondApproval.setApprovalOpinion("第二级拒绝");
        applicationService.processApproval(applicationId, secondApproval, secondApproverId, "第二级审批人");
        
        // 4. 验证流程终止
        application = applicationMapper.selectById(applicationId);
        assertEquals(4, application.getStatus()); // 审批拒绝
        assertEquals(3, application.getApprovalStatus()); // 审批拒绝
        assertNull(application.getCurrentApproverId());
        
        // 5. 验证审批记录
        List<ApprovalRecord> records = getApprovalRecords(applicationId);
        assertEquals(2, records.size());
        assertEquals(1, records.get(0).getApprovalResult()); // 第一级通过
        assertEquals(2, records.get(1).getApprovalResult()); // 第二级拒绝
    }
    
    @Test
    @DisplayName("审批记录完整性 - 所有必需字段都被记录")
    void testApprovalRecord_AllRequiredFieldsRecorded() {
        // 1. 创建申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 2. 执行审批
        MaterialApplication application = applicationMapper.selectById(applicationId);
        Long approverId = application.getCurrentApproverId();
        
        ApprovalProcessRequest approvalRequest = new ApprovalProcessRequest();
        approvalRequest.setApprovalResult(1);
        approvalRequest.setApprovalOpinion("详细的审批意见");
        
        applicationService.processApproval(applicationId, approvalRequest, approverId, "审批人姓名");
        
        // 3. 验证审批记录完整性
        List<ApprovalRecord> records = getApprovalRecords(applicationId);
        assertEquals(1, records.size());
        
        ApprovalRecord record = records.get(0);
        assertNotNull(record.getId());
        assertEquals(applicationId, record.getApplicationId());
        assertNotNull(record.getApplicationNo());
        assertEquals(approverId, record.getApproverId());
        assertEquals("审批人姓名", record.getApproverName());
        assertEquals(1, record.getApprovalLevel());
        assertEquals(1, record.getApprovalResult());
        assertEquals("详细的审批意见", record.getApprovalOpinion());
        assertNotNull(record.getApprovalTime());
        assertNotNull(record.getCreatedTime());
    }
    
    // ==================== 库存检查测试 ====================
    
    @Test
    @DisplayName("库存检查 - 库存充足时允许创建申请")
    void testInventoryCheck_AllowsApplicationWhenStockSufficient() {
        // Mock库存充足
        when(inventoryClient.checkStockAvailability(eq(1L), eq(new BigDecimal("10")))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        
        // 应该成功创建
        assertDoesNotThrow(() -> {
            Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
            assertNotNull(applicationId);
        });
    }
    
    @Test
    @DisplayName("库存检查 - 库存不足时拒绝创建申请")
    void testInventoryCheck_RejectsApplicationWhenStockInsufficient() {
        // Mock库存不足
        when(inventoryClient.checkStockAvailability(eq(1L), eq(new BigDecimal("10")))).thenReturn(false);
        when(inventoryClient.getAvailableStock(eq(1L))).thenReturn(new BigDecimal("5"));
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        
        // 应该抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(request, testUserId, testUserName, testDept);
        });
        
        assertTrue(exception.getMessage().contains("库存不足"));
        assertTrue(exception.getMessage().contains("当前可用库存: 5"));
    }
    
    @Test
    @DisplayName("库存检查 - 多个物料时检查所有物料库存")
    void testInventoryCheck_ChecksAllMaterialsInMultiItemApplication() {
        // Mock第一个物料库存充足，第二个物料库存不足
        when(inventoryClient.checkStockAvailability(eq(1L), any(BigDecimal.class))).thenReturn(true);
        when(inventoryClient.checkStockAvailability(eq(2L), any(BigDecimal.class))).thenReturn(false);
        when(inventoryClient.getAvailableStock(eq(2L))).thenReturn(new BigDecimal("3"));
        
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(1);
        request.setUsagePurpose("测试用途");
        request.setUsageLocation("测试地点");
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        List<CreateApplicationRequest.ApplicationItemRequest> items = new ArrayList<>();
        
        CreateApplicationRequest.ApplicationItemRequest item1 = new CreateApplicationRequest.ApplicationItemRequest();
        item1.setMaterialId(1L);
        item1.setApplyQuantity(new BigDecimal("5"));
        items.add(item1);
        
        CreateApplicationRequest.ApplicationItemRequest item2 = new CreateApplicationRequest.ApplicationItemRequest();
        item2.setMaterialId(2L);
        item2.setApplyQuantity(new BigDecimal("10"));
        items.add(item2);
        
        request.setItems(items);
        
        // 应该因为第二个物料库存不足而失败
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(request, testUserId, testUserName, testDept);
        });
        
        assertTrue(exception.getMessage().contains("库存不足"));
    }
    
    @Test
    @DisplayName("库存检查 - 申请数量为零时拒绝")
    void testInventoryCheck_RejectsZeroQuantity() {
        CreateApplicationRequest request = createNormalApplicationRequest();
        request.getItems().get(0).setApplyQuantity(BigDecimal.ZERO);
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(request, testUserId, testUserName, testDept);
        });
        
        assertEquals("申请数量必须大于0", exception.getMessage());
    }
    
    @Test
    @DisplayName("库存检查 - 申请数量为负数时拒绝")
    void testInventoryCheck_RejectsNegativeQuantity() {
        CreateApplicationRequest request = createNormalApplicationRequest();
        request.getItems().get(0).setApplyQuantity(new BigDecimal("-5"));
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(request, testUserId, testUserName, testDept);
        });
        
        assertEquals("申请数量必须大于0", exception.getMessage());
    }
    
    // ==================== 权限控制测试 ====================
    
    @Test
    @DisplayName("权限控制 - 只有当前审批人可以审批")
    void testPermissionControl_OnlyCurrentApproverCanApprove() {
        // 1. 创建申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 2. 使用错误的审批人ID尝试审批
        MaterialApplication application = applicationMapper.selectById(applicationId);
        Long correctApproverId = application.getCurrentApproverId();
        Long wrongApproverId = correctApproverId + 999L; // 错误的审批人ID
        
        ApprovalProcessRequest approvalRequest = new ApprovalProcessRequest();
        approvalRequest.setApprovalResult(1);
        approvalRequest.setApprovalOpinion("同意");
        
        // 应该抛出权限异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.processApproval(applicationId, approvalRequest, wrongApproverId, "错误审批人");
        });
        
        assertTrue(exception.getMessage().contains("不是当前审批人"));
    }
    
    @Test
    @DisplayName("权限控制 - 只有申请人可以取消申请")
    void testPermissionControl_OnlyApplicantCanCancelApplication() {
        // 1. 创建申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 2. 使用其他用户ID尝试取消
        Long otherUserId = testUserId + 999L;
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.cancelApplication(applicationId, otherUserId);
        });
        
        assertEquals("只有申请人可以取消申请", exception.getMessage());
    }
    
    @Test
    @DisplayName("权限控制 - 申请人可以成功取消自己的申请")
    void testPermissionControl_ApplicantCanCancelOwnApplication() {
        // 1. 创建申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 2. 申请人取消申请
        assertDoesNotThrow(() -> {
            applicationService.cancelApplication(applicationId, testUserId);
        });
        
        // 3. 验证状态
        MaterialApplication application = applicationMapper.selectById(applicationId);
        assertEquals(7, application.getStatus()); // 已取消
    }
    
    @Test
    @DisplayName("权限控制 - 已审批通过的申请不能取消")
    void testPermissionControl_CannotCancelApprovedApplication() {
        // 1. 创建并审批通过申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        MaterialApplication application = applicationMapper.selectById(applicationId);
        Long approverId = application.getCurrentApproverId();
        
        ApprovalProcessRequest approvalRequest = new ApprovalProcessRequest();
        approvalRequest.setApprovalResult(1);
        approvalRequest.setApprovalOpinion("同意");
        applicationService.processApproval(applicationId, approvalRequest, approverId, "审批人");
        
        // 2. 尝试取消已审批通过的申请
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.cancelApplication(applicationId, testUserId);
        });
        
        assertEquals("当前状态不允许取消", exception.getMessage());
    }
    
    @Test
    @DisplayName("权限控制 - 已出库的申请不能取消")
    void testPermissionControl_CannotCancelStockOutApplication() {
        // 1. 创建申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 2. 手动修改状态为已出库
        MaterialApplication application = applicationMapper.selectById(applicationId);
        application.setStatus(5); // 已出库
        applicationMapper.updateById(application);
        
        // 3. 尝试取消
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.cancelApplication(applicationId, testUserId);
        });
        
        assertEquals("当前状态不允许取消", exception.getMessage());
    }
    
    @Test
    @DisplayName("权限控制 - 不在审批中的申请不能审批")
    void testPermissionControl_CannotApproveNonPendingApplication() {
        // 1. 创建申请
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        CreateApplicationRequest request = createNormalApplicationRequest();
        Long applicationId = applicationService.createApplication(request, testUserId, testUserName, testDept);
        
        // 2. 手动修改状态为已完成
        MaterialApplication application = applicationMapper.selectById(applicationId);
        Long approverId = application.getCurrentApproverId();
        application.setStatus(6); // 已完成
        applicationMapper.updateById(application);
        
        // 3. 尝试审批
        ApprovalProcessRequest approvalRequest = new ApprovalProcessRequest();
        approvalRequest.setApprovalResult(1);
        approvalRequest.setApprovalOpinion("同意");
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.processApproval(applicationId, approvalRequest, approverId, "审批人");
        });
        
        assertTrue(exception.getMessage().contains("不在审批中状态"));
    }
    
    // ==================== 辅助方法 ====================
    
    private CreateApplicationRequest createNormalApplicationRequest() {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(1); // 普通领用
        request.setUsagePurpose("实验使用");
        request.setUsageLocation("实验室A");
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal("10"));
        
        request.setItems(Collections.singletonList(item));
        
        return request;
    }
    
    private CreateApplicationRequest createHazardousApplicationRequest() {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose("危化品实验使用");
        request.setUsageLocation("危化品实验室");
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(2L); // 危化品
        item.setApplyQuantity(new BigDecimal("5"));
        
        request.setItems(Collections.singletonList(item));
        
        return request;
    }
    
    private List<ApprovalRecord> getApprovalRecords(Long applicationId) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getApplicationId, applicationId)
               .orderByAsc(ApprovalRecord::getApprovalLevel);
        return recordMapper.selectList(wrapper);
    }
}
