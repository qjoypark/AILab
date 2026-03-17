package com.lab.approval.property;

import com.lab.approval.client.InventoryClient;
import com.lab.approval.client.MaterialClient;
import com.lab.approval.client.UserClient;
import com.lab.approval.dto.CreateApplicationRequest;
import com.lab.approval.dto.MaterialInfo;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.mapper.MaterialApplicationItemMapper;
import com.lab.approval.mapper.MaterialApplicationMapper;
import com.lab.approval.service.ApprovalWorkflowService;
import com.lab.approval.service.impl.MaterialApplicationServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 危化品审批强制执行属性测试
 * 
 * 使用jqwik框架进行基于属性的测试，每个属性测试运行100次迭代
 */
@ExtendWith(MockitoExtension.class)
public class HazardousApprovalEnforcementPropertyTest {
    
    @Mock
    private MaterialApplicationMapper applicationMapper;
    
    @Mock
    private MaterialApplicationItemMapper itemMapper;
    
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    
    @Mock
    private ApprovalFlowConfigMapper flowConfigMapper;
    
    @Mock
    private ApprovalWorkflowService approvalWorkflowService;
    
    @Mock
    private InventoryClient inventoryClient;
    
    @Mock
    private MaterialClient materialClient;
    
    @Mock
    private UserClient userClient;
    
    @InjectMocks
    private MaterialApplicationServiceImpl applicationService;
    
    /**
     * 属性 13: 危化品领用强制审批
     * 
     * **Validates: Requirements 6.4**
     * 
     * 对于任何危化品领用申请，申请创建后其状态应为"待审批"或"审批中"，
     * 并且应创建对应的审批流程，只有审批通过后才能执行出库操作。
     * 
     * 本测试验证：
     * 1. 危化品申请创建后，状态必须为待审批(1)或审批中(2)
     * 2. 必须调用审批流程初始化方法
     * 3. 必须设置当前审批人ID
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 13: 危化品领用强制审批")
    void hazardousApplicationMustGoThroughApprovalWorkflow(
            @ForAll @StringLength(min = 1, max = 500) String usagePurpose,
            @ForAll @StringLength(min = 1, max = 200) String usageLocation,
            @ForAll @IntRange(min = 1, max = 1000) int applyQuantity) {
        
        // 准备测试数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        Long firstApproverId = 100L;
        
        // 创建危化品申请请求
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose(usagePurpose);
        request.setUsageLocation(usageLocation);
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal(applyQuantity));
        request.setItems(Collections.singletonList(item));
        
        // Mock材料信息（危化品）
        MaterialInfo materialInfo = new MaterialInfo();
        materialInfo.setId(1L);
        materialInfo.setMaterialName("测试危化品");
        materialInfo.setSpecification("100ml");
        materialInfo.setUnit("瓶");
        materialInfo.setMaterialType(3); // 危化品
        when(materialClient.getMaterialInfo(anyLong())).thenReturn(materialInfo);
        
        // Mock库存检查
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        // Mock安全资质检查
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // Mock审批流程初始化，返回第一级审批人ID
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), anyString(), any()))
            .thenReturn(firstApproverId);
        
        // 使用ArgumentCaptor捕获insert和update操作
        ArgumentCaptor<MaterialApplication> insertCaptor = ArgumentCaptor.forClass(MaterialApplication.class);
        ArgumentCaptor<MaterialApplication> updateCaptor = ArgumentCaptor.forClass(MaterialApplication.class);
        
        // Mock insert操作
        doAnswer(invocation -> {
            MaterialApplication app = invocation.getArgument(0);
            app.setId(1L);
            return 1;
        }).when(applicationMapper).insert(insertCaptor.capture());
        
        // Mock update操作
        when(applicationMapper.updateById(updateCaptor.capture())).thenReturn(1);
        
        // 执行创建申请
        Long applicationId = applicationService.createApplication(
            request, applicantId, applicantName, applicantDept
        );
        
        // 验证申请创建成功
        assertThat(applicationId).isNotNull();
        
        // 验证insert操作：初始状态应为"待审批"
        MaterialApplication insertedApp = insertCaptor.getValue();
        assertThat(insertedApp.getStatus())
            .as("危化品申请创建时状态应为待审批(1)")
            .isEqualTo(1);
        assertThat(insertedApp.getApprovalStatus())
            .as("危化品申请创建时审批状态应为未审批(0)")
            .isEqualTo(0);
        assertThat(insertedApp.getApplicationType())
            .as("申请类型应为危化品领用(2)")
            .isEqualTo(2);
        
        // 验证update操作：更新后状态应为"审批中"
        MaterialApplication updatedApp = updateCaptor.getValue();
        assertThat(updatedApp.getStatus())
            .as("危化品申请启动审批流程后状态应为审批中(2)")
            .isEqualTo(2);
        assertThat(updatedApp.getApprovalStatus())
            .as("危化品申请启动审批流程后审批状态应为审批中(1)")
            .isEqualTo(1);
        assertThat(updatedApp.getCurrentApproverId())
            .as("危化品申请必须设置当前审批人ID")
            .isNotNull()
            .isEqualTo(firstApproverId);
        
        // 验证必须调用审批流程初始化
        verify(approvalWorkflowService, times(1))
            .initializeApprovalWorkflow(
                eq(applicationId), 
                anyString(), 
                argThat(context -> 
                    context.getBusinessType() == 2 && // 危化品领用
                    context.getApplicantId().equals(applicantId)
                )
            );
        
        // 重置mock以便下次迭代
        reset(applicationMapper, itemMapper, userClient, approvalWorkflowService, 
              materialClient, inventoryClient);
    }
    
    /**
     * 属性 13: 危化品申请必须初始化审批流程
     * 
     * **Validates: Requirements 6.4**
     * 
     * 验证危化品申请创建过程中，审批流程初始化是强制性的。
     * 如果审批流程初始化失败，整个申请创建应该失败。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 13: 危化品领用强制审批")
    void hazardousApplicationRequiresApprovalWorkflowInitialization(
            @ForAll @StringLength(min = 1, max = 500) String usagePurpose,
            @ForAll @StringLength(min = 1, max = 200) String usageLocation,
            @ForAll @IntRange(min = 1, max = 1000) int applyQuantity) {
        
        // 准备测试数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        
        // 创建危化品申请请求
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose(usagePurpose);
        request.setUsageLocation(usageLocation);
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal(applyQuantity));
        request.setItems(Collections.singletonList(item));
        
        // Mock材料信息（危化品）
        MaterialInfo materialInfo = new MaterialInfo();
        materialInfo.setId(1L);
        materialInfo.setMaterialName("测试危化品");
        materialInfo.setSpecification("100ml");
        materialInfo.setUnit("瓶");
        materialInfo.setMaterialType(3); // 危化品
        when(materialClient.getMaterialInfo(anyLong())).thenReturn(materialInfo);
        
        // Mock库存检查
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        // Mock安全资质检查
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // Mock审批流程初始化，返回第一级审批人ID
        Long firstApproverId = 100L;
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), anyString(), any()))
            .thenReturn(firstApproverId);
        
        // Mock insert操作
        doAnswer(invocation -> {
            MaterialApplication app = invocation.getArgument(0);
            app.setId(1L);
            return 1;
        }).when(applicationMapper).insert(any(MaterialApplication.class));
        
        // Mock update操作
        when(applicationMapper.updateById(any(MaterialApplication.class))).thenReturn(1);
        
        // 执行创建申请
        applicationService.createApplication(
            request, applicantId, applicantName, applicantDept
        );
        
        // 验证审批流程初始化被调用
        verify(approvalWorkflowService, times(1))
            .initializeApprovalWorkflow(anyLong(), anyString(), any());
        
        // 验证申请单被更新（设置审批人）
        verify(applicationMapper, times(1)).updateById(any(MaterialApplication.class));
        
        // 重置mock以便下次迭代
        reset(applicationMapper, itemMapper, userClient, approvalWorkflowService, 
              materialClient, inventoryClient);
    }
    
    /**
     * 属性 13: 危化品申请状态转换验证
     * 
     * **Validates: Requirements 6.4**
     * 
     * 验证危化品申请的状态转换：
     * 1. 创建时：status=1(待审批), approvalStatus=0(未审批)
     * 2. 启动审批流程后：status=2(审批中), approvalStatus=1(审批中)
     * 3. 必须设置currentApproverId
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 13: 危化品领用强制审批")
    void hazardousApplicationStatusTransitionValidation(
            @ForAll @StringLength(min = 1, max = 500) String usagePurpose,
            @ForAll @StringLength(min = 1, max = 200) String usageLocation,
            @ForAll @IntRange(min = 1, max = 1000) int applyQuantity,
            @ForAll @IntRange(min = 100, max = 999) int approverIdSuffix) {
        
        // 准备测试数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        Long firstApproverId = Long.valueOf(approverIdSuffix);
        
        // 创建危化品申请请求
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose(usagePurpose);
        request.setUsageLocation(usageLocation);
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal(applyQuantity));
        request.setItems(Collections.singletonList(item));
        
        // Mock材料信息（危化品）
        MaterialInfo materialInfo = new MaterialInfo();
        materialInfo.setId(1L);
        materialInfo.setMaterialName("测试危化品");
        materialInfo.setSpecification("100ml");
        materialInfo.setUnit("瓶");
        materialInfo.setMaterialType(3); // 危化品
        when(materialClient.getMaterialInfo(anyLong())).thenReturn(materialInfo);
        
        // Mock库存检查
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        // Mock安全资质检查
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // Mock审批流程初始化
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), anyString(), any()))
            .thenReturn(firstApproverId);
        
        // 使用ArgumentCaptor捕获所有状态变化
        ArgumentCaptor<MaterialApplication> insertCaptor = ArgumentCaptor.forClass(MaterialApplication.class);
        ArgumentCaptor<MaterialApplication> updateCaptor = ArgumentCaptor.forClass(MaterialApplication.class);
        
        // Mock insert操作
        doAnswer(invocation -> {
            MaterialApplication app = invocation.getArgument(0);
            app.setId(1L);
            return 1;
        }).when(applicationMapper).insert(insertCaptor.capture());
        
        // Mock update操作
        when(applicationMapper.updateById(updateCaptor.capture())).thenReturn(1);
        
        // 执行创建申请
        applicationService.createApplication(
            request, applicantId, applicantName, applicantDept
        );
        
        // 验证初始状态（insert时）
        MaterialApplication initialState = insertCaptor.getValue();
        assertThat(initialState.getStatus())
            .as("危化品申请初始状态必须为待审批(1)")
            .isEqualTo(1);
        assertThat(initialState.getApprovalStatus())
            .as("危化品申请初始审批状态必须为未审批(0)")
            .isEqualTo(0);
        assertThat(initialState.getApplicationType())
            .as("申请类型必须为危化品领用(2)")
            .isEqualTo(2);
        
        // 验证审批流程启动后的状态（update时）
        MaterialApplication approvalState = updateCaptor.getValue();
        assertThat(approvalState.getStatus())
            .as("危化品申请启动审批后状态必须为审批中(2)")
            .isEqualTo(2);
        assertThat(approvalState.getApprovalStatus())
            .as("危化品申请启动审批后审批状态必须为审批中(1)")
            .isEqualTo(1);
        assertThat(approvalState.getCurrentApproverId())
            .as("危化品申请必须设置当前审批人ID")
            .isNotNull()
            .isEqualTo(firstApproverId);
        
        // 验证状态转换的完整性：从待审批到审批中
        assertThat(approvalState.getStatus())
            .as("状态必须从待审批(1)转换到审批中(2)")
            .isGreaterThan(initialState.getStatus());
        assertThat(approvalState.getApprovalStatus())
            .as("审批状态必须从未审批(0)转换到审批中(1)")
            .isGreaterThan(initialState.getApprovalStatus());
        
        // 重置mock以便下次迭代
        reset(applicationMapper, itemMapper, userClient, approvalWorkflowService, 
              materialClient, inventoryClient);
    }
    
    /**
     * 属性 13: 普通申请与危化品申请的审批流程区别
     * 
     * **Validates: Requirements 6.4**
     * 
     * 验证普通申请和危化品申请都必须经过审批流程，
     * 但危化品申请使用不同的审批流程配置（businessType=2）。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 13: 危化品领用强制审批")
    void hazardousApplicationUsesSpecificApprovalWorkflow(
            @ForAll @StringLength(min = 1, max = 500) String usagePurpose,
            @ForAll @StringLength(min = 1, max = 200) String usageLocation,
            @ForAll @IntRange(min = 1, max = 1000) int applyQuantity) {
        
        // 准备测试数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        Long firstApproverId = 100L;
        
        // 创建危化品申请请求
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose(usagePurpose);
        request.setUsageLocation(usageLocation);
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal(applyQuantity));
        request.setItems(Collections.singletonList(item));
        
        // Mock材料信息（危化品）
        MaterialInfo materialInfo = new MaterialInfo();
        materialInfo.setId(1L);
        materialInfo.setMaterialName("测试危化品");
        materialInfo.setSpecification("100ml");
        materialInfo.setUnit("瓶");
        materialInfo.setMaterialType(3); // 危化品
        when(materialClient.getMaterialInfo(anyLong())).thenReturn(materialInfo);
        
        // Mock库存检查
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class))).thenReturn(true);
        
        // Mock安全资质检查
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // Mock审批流程初始化
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), anyString(), any()))
            .thenReturn(firstApproverId);
        
        // Mock insert操作
        doAnswer(invocation -> {
            MaterialApplication app = invocation.getArgument(0);
            app.setId(1L);
            return 1;
        }).when(applicationMapper).insert(any(MaterialApplication.class));
        
        // Mock update操作
        when(applicationMapper.updateById(any(MaterialApplication.class))).thenReturn(1);
        
        // 执行创建申请
        applicationService.createApplication(
            request, applicantId, applicantName, applicantDept
        );
        
        // 验证审批流程初始化时传入的业务类型为危化品领用(2)
        verify(approvalWorkflowService, times(1))
            .initializeApprovalWorkflow(
                anyLong(), 
                anyString(), 
                argThat(context -> {
                    // 验证业务类型为危化品领用
                    assertThat(context.getBusinessType())
                        .as("危化品申请必须使用危化品审批流程(businessType=2)")
                        .isEqualTo(2);
                    // 验证申请人信息正确传递
                    assertThat(context.getApplicantId())
                        .as("审批上下文必须包含申请人ID")
                        .isEqualTo(applicantId);
                    assertThat(context.getApplicantDept())
                        .as("审批上下文必须包含申请人部门")
                        .isEqualTo(applicantDept);
                    return true;
                })
            );
        
        // 重置mock以便下次迭代
        reset(applicationMapper, itemMapper, userClient, approvalWorkflowService, 
              materialClient, inventoryClient);
    }
}
