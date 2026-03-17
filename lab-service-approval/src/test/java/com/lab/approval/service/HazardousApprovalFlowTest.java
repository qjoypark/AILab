package com.lab.approval.service;

import com.lab.approval.client.InventoryClient;
import com.lab.approval.client.MaterialClient;
import com.lab.approval.client.UserClient;
import com.lab.approval.dto.ApprovalProcessRequest;
import com.lab.approval.dto.CreateApplicationRequest;
import com.lab.approval.dto.MaterialInfo;
import com.lab.approval.entity.ApprovalFlowConfig;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.mapper.MaterialApplicationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 危化品审批流程测试
 * 验证需求 6.4: 危化品申请自动使用多级审批流程
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HazardousApprovalFlowTest {
    
    @Autowired
    private MaterialApplicationService applicationService;
    
    @Autowired
    private MaterialApplicationMapper applicationMapper;
    
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;
    
    @Autowired
    private ApprovalFlowConfigMapper flowConfigMapper;
    
    @MockBean
    private InventoryClient inventoryClient;
    
    @MockBean
    private MaterialClient materialClient;
    
    @MockBean
    private UserClient userClient;
    
    private Long testUserId = 100L;
    private String testUserName = "测试用户";
    private String testDept = "农学院";
    
    @BeforeEach
    void setUp() {
        // 初始化危化品审批流程配置
        setupHazardousApprovalFlow();
        
        // Mock危化品材料信息
        setupHazardousMaterialMock();
        
        // Mock库存检查
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        when(inventoryClient.getAvailableStock(anyLong())).thenReturn(new BigDecimal("100"));
        
        // Mock用户安全资质验证
        when(userClient.checkSafetyCertification(anyLong())).thenReturn(true);
    }
    
    @Test
    @DisplayName("危化品申请应自动使用三级审批流程")
    void testHazardousApplication_ShouldUseThreeLevelApprovalFlow() {
        // Given: 创建危化品申请
        CreateApplicationRequest request = createHazardousApplicationRequest();
        
        // When: 提交申请
        Long applicationId = applicationService.createApplication(
            request, testUserId, testUserName, testDept
        );
        
        // Then: 验证申请单创建成功
        assertNotNull(applicationId);
        
        MaterialApplication application = applicationMapper.selectById(applicationId);
        assertNotNull(application);
        assertEquals(2, application.getApplicationType(), "申请类型应为危化品领用");
        assertEquals(2, application.getStatus(), "状态应为审批中");
        assertEquals(1, application.getApprovalStatus(), "审批状态应为审批中");
        assertNotNull(application.getCurrentApproverId(), "应分配第一级审批人");
        
        // 验证使用的是危化品审批流程配置
        ApprovalFlowConfig flowConfig = flowConfigMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovalFlowConfig>()
                .eq(ApprovalFlowConfig::getBusinessType, 2)
                .eq(ApprovalFlowConfig::getStatus, 1)
        );
        assertNotNull(flowConfig, "应存在危化品审批流程配置");
        assertTrue(flowConfig.getFlowDefinition().contains("LAB_MANAGER"), "应包含实验室负责人");
        assertTrue(flowConfig.getFlowDefinition().contains("CENTER_ADMIN"), "应包含中心管理员");
        assertTrue(flowConfig.getFlowDefinition().contains("ADMIN"), "应包含安全管理员");
    }
    
    @Test
    @DisplayName("危化品审批流程应包含三个层级：实验室负责人→中心管理员→安全管理员")
    void testHazardousApprovalFlow_ShouldHaveThreeLevels() {
        // Given: 创建危化品申请
        CreateApplicationRequest request = createHazardousApplicationRequest();
        Long applicationId = applicationService.createApplication(
            request, testUserId, testUserName, testDept
        );
        
        MaterialApplication application = applicationMapper.selectById(applicationId);
        Long level1ApproverId = application.getCurrentApproverId();
        
        // When: 第一级审批通过（实验室负责人）
        ApprovalProcessRequest approval1 = new ApprovalProcessRequest();
        approval1.setApprovalResult(1); // 通过
        approval1.setApprovalOpinion("实验室负责人同意");
        
        applicationService.processApproval(applicationId, approval1, level1ApproverId, "实验室负责人");
        
        // Then: 验证流转到第二级
        application = applicationMapper.selectById(applicationId);
        assertEquals(2, application.getStatus(), "状态应仍为审批中");
        assertEquals(1, application.getApprovalStatus(), "审批状态应仍为审批中");
        assertNotNull(application.getCurrentApproverId(), "应分配第二级审批人");
        assertNotEquals(level1ApproverId, application.getCurrentApproverId(), "第二级审批人应不同于第一级");
        
        // 验证第一级审批记录
        List<ApprovalRecord> records = approvalRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getApplicationId, applicationId)
                .orderByAsc(ApprovalRecord::getApprovalLevel)
        );
        assertEquals(1, records.size(), "应有1条审批记录");
        assertEquals(1, records.get(0).getApprovalLevel(), "应为第1级审批");
        assertEquals(1, records.get(0).getApprovalResult(), "审批结果应为通过");
        
        // When: 第二级审批通过（中心管理员）
        Long level2ApproverId = application.getCurrentApproverId();
        ApprovalProcessRequest approval2 = new ApprovalProcessRequest();
        approval2.setApprovalResult(1); // 通过
        approval2.setApprovalOpinion("中心管理员同意");
        
        applicationService.processApproval(applicationId, approval2, level2ApproverId, "中心管理员");
        
        // Then: 验证流转到第三级
        application = applicationMapper.selectById(applicationId);
        assertEquals(2, application.getStatus(), "状态应仍为审批中");
        assertEquals(1, application.getApprovalStatus(), "审批状态应仍为审批中");
        assertNotNull(application.getCurrentApproverId(), "应分配第三级审批人");
        assertNotEquals(level2ApproverId, application.getCurrentApproverId(), "第三级审批人应不同于第二级");
        
        // 验证第二级审批记录
        records = approvalRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getApplicationId, applicationId)
                .orderByAsc(ApprovalRecord::getApprovalLevel)
        );
        assertEquals(2, records.size(), "应有2条审批记录");
        assertEquals(2, records.get(1).getApprovalLevel(), "应为第2级审批");
        assertEquals(1, records.get(1).getApprovalResult(), "审批结果应为通过");
        
        // When: 第三级审批通过（安全管理员）
        Long level3ApproverId = application.getCurrentApproverId();
        ApprovalProcessRequest approval3 = new ApprovalProcessRequest();
        approval3.setApprovalResult(1); // 通过
        approval3.setApprovalOpinion("安全管理员同意");
        
        applicationService.processApproval(applicationId, approval3, level3ApproverId, "安全管理员");
        
        // Then: 验证审批完成
        application = applicationMapper.selectById(applicationId);
        assertEquals(3, application.getStatus(), "状态应为审批通过");
        assertEquals(2, application.getApprovalStatus(), "审批状态应为审批通过");
        assertNull(application.getCurrentApproverId(), "审批完成后应无当前审批人");
        
        // 验证第三级审批记录
        records = approvalRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getApplicationId, applicationId)
                .orderByAsc(ApprovalRecord::getApprovalLevel)
        );
        assertEquals(3, records.size(), "应有3条审批记录");
        assertEquals(3, records.get(2).getApprovalLevel(), "应为第3级审批");
        assertEquals(1, records.get(2).getApprovalResult(), "审批结果应为通过");
    }
    
    @Test
    @DisplayName("危化品审批在任一级被拒绝应终止流程")
    void testHazardousApprovalFlow_ShouldTerminateWhenRejected() {
        // Given: 创建危化品申请
        CreateApplicationRequest request = createHazardousApplicationRequest();
        Long applicationId = applicationService.createApplication(
            request, testUserId, testUserName, testDept
        );
        
        MaterialApplication application = applicationMapper.selectById(applicationId);
        Long level1ApproverId = application.getCurrentApproverId();
        
        // When: 第一级审批拒绝
        ApprovalProcessRequest approval = new ApprovalProcessRequest();
        approval.setApprovalResult(2); // 拒绝
        approval.setApprovalOpinion("不符合安全要求");
        
        applicationService.processApproval(applicationId, approval, level1ApproverId, "实验室负责人");
        
        // Then: 验证审批终止
        application = applicationMapper.selectById(applicationId);
        assertEquals(4, application.getStatus(), "状态应为审批拒绝");
        assertEquals(3, application.getApprovalStatus(), "审批状态应为审批拒绝");
        assertNull(application.getCurrentApproverId(), "审批终止后应无当前审批人");
        
        // 验证审批记录
        List<ApprovalRecord> records = approvalRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getApplicationId, applicationId)
        );
        assertEquals(1, records.size(), "应只有1条审批记录");
        assertEquals(2, records.get(0).getApprovalResult(), "审批结果应为拒绝");
    }
    
    @Test
    @DisplayName("危化品审批在第二级被拒绝应终止流程")
    void testHazardousApprovalFlow_ShouldTerminateWhenRejectedAtLevel2() {
        // Given: 创建危化品申请并通过第一级审批
        CreateApplicationRequest request = createHazardousApplicationRequest();
        Long applicationId = applicationService.createApplication(
            request, testUserId, testUserName, testDept
        );
        
        MaterialApplication application = applicationMapper.selectById(applicationId);
        Long level1ApproverId = application.getCurrentApproverId();
        
        // 第一级审批通过
        ApprovalProcessRequest approval1 = new ApprovalProcessRequest();
        approval1.setApprovalResult(1);
        approval1.setApprovalOpinion("实验室负责人同意");
        applicationService.processApproval(applicationId, approval1, level1ApproverId, "实验室负责人");
        
        // When: 第二级审批拒绝
        application = applicationMapper.selectById(applicationId);
        Long level2ApproverId = application.getCurrentApproverId();
        
        ApprovalProcessRequest approval2 = new ApprovalProcessRequest();
        approval2.setApprovalResult(2); // 拒绝
        approval2.setApprovalOpinion("中心管理员不同意");
        
        applicationService.processApproval(applicationId, approval2, level2ApproverId, "中心管理员");
        
        // Then: 验证审批终止
        application = applicationMapper.selectById(applicationId);
        assertEquals(4, application.getStatus(), "状态应为审批拒绝");
        assertEquals(3, application.getApprovalStatus(), "审批状态应为审批拒绝");
        assertNull(application.getCurrentApproverId(), "审批终止后应无当前审批人");
        
        // 验证审批记录
        List<ApprovalRecord> records = approvalRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getApplicationId, applicationId)
                .orderByAsc(ApprovalRecord::getApprovalLevel)
        );
        assertEquals(2, records.size(), "应有2条审批记录");
        assertEquals(1, records.get(0).getApprovalResult(), "第一级应为通过");
        assertEquals(2, records.get(1).getApprovalResult(), "第二级应为拒绝");
    }
    
    // ==================== 辅助方法 ====================
    
    private void setupHazardousApprovalFlow() {
        // 创建危化品审批流程配置
        ApprovalFlowConfig hazardousFlow = new ApprovalFlowConfig();
        hazardousFlow.setFlowCode("HAZARDOUS_APPLY");
        hazardousFlow.setFlowName("危化品领用审批流程");
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
    
    private void setupHazardousMaterialMock() {
        MaterialInfo hazardousMaterial = new MaterialInfo();
        hazardousMaterial.setMaterialId(1L);
        hazardousMaterial.setMaterialName("危险化学品");
        hazardousMaterial.setSpecification("500ml");
        hazardousMaterial.setUnit("瓶");
        hazardousMaterial.setMaterialType(3); // 危化品
        hazardousMaterial.setIsControlled(1); // 易制毒
        
        when(materialClient.getMaterialInfo(1L)).thenReturn(hazardousMaterial);
    }
    
    private CreateApplicationRequest createHazardousApplicationRequest() {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose("危化品实验使用");
        request.setUsageLocation("危化品实验室");
        
        List<CreateApplicationRequest.ApplicationItemRequest> items = new ArrayList<>();
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal("2"));
        items.add(item);
        
        request.setItems(items);
        return request;
    }
}
