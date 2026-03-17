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
import com.lab.common.exception.BusinessException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 危化品申请验证属性测试
 * 
 * 使用jqwik框架进行基于属性的测试，每个属性测试运行100次迭代
 */
@ExtendWith(MockitoExtension.class)
public class HazardousApplicationPropertyTest {
    
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
     * 属性 12: 危化品申请必需字段验证
     * 
     * **Validates: Requirements 6.3**
     * 
     * 当用户申请领用危化品时，系统应要求填写领用数量、用途、使用地点。
     * 如果缺少这些必需字段，系统应拒绝申请。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 12: 危化品申请必需字段验证")
    void hazardousApplicationRequiredFieldsValidation(
            @ForAll @StringLength(min = 1, max = 500) String usagePurpose,
            @ForAll @StringLength(min = 1, max = 200) String usageLocation,
            @ForAll @IntRange(min = 1, max = 1000) int applyQuantity) {
        
        // 准备测试数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        
        // 创建危化品申请请求（包含所有必需字段）
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose(usagePurpose);
        request.setUsageLocation(usageLocation);
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal(applyQuantity));
        request.setItems(Collections.singletonList(item));
        
        // Mock材料信息
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
        
        // Mock审批流程
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), anyString(), any()))
            .thenReturn(100L);
        
        // Mock insert操作
        doAnswer(invocation -> {
            MaterialApplication app = invocation.getArgument(0);
            app.setId(1L);
            return 1;
        }).when(applicationMapper).insert(any(MaterialApplication.class));
        
        // 执行创建申请
        Long applicationId = applicationService.createApplication(
            request, applicantId, applicantName, applicantDept
        );
        
        // 验证申请创建成功
        assertThat(applicationId).isNotNull();
        
        // 验证调用了安全资质检查
        verify(userClient, times(1)).checkSafetyCertification(applicantId);
        
        // 验证创建了申请单
        verify(applicationMapper, times(1)).insert(any(MaterialApplication.class));
        verify(applicationMapper, times(1)).updateById(any(MaterialApplication.class));
        
        // 验证创建了申请明细
        verify(itemMapper, times(1)).insert(any());
        
        // 验证启动了审批流程
        verify(approvalWorkflowService, times(1))
            .initializeApprovalWorkflow(anyLong(), anyString(), any());
        
        // 重置mock以便下次迭代
        reset(applicationMapper, itemMapper, userClient, approvalWorkflowService);
    }
    
    /**
     * 属性 12: 危化品申请缺少用途说明时应拒绝
     * 
     * **Validates: Requirements 6.3**
     * 
     * 当用户申请领用危化品时，如果缺少用途说明，系统应拒绝申请。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 12: 危化品申请必需字段验证")
    void hazardousApplicationShouldRejectWhenUsagePurposeMissing(
            @ForAll @StringLength(min = 1, max = 200) String usageLocation,
            @ForAll @IntRange(min = 1, max = 1000) int applyQuantity) {
        
        // 准备测试数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        
        // 创建危化品申请请求（缺少用途说明）
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose(null); // 缺少用途说明
        request.setUsageLocation(usageLocation);
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal(applyQuantity));
        request.setItems(Collections.singletonList(item));
        
        // Mock安全资质检查
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // 执行并验证异常
        assertThatThrownBy(() -> {
            applicationService.createApplication(request, applicantId, applicantName, applicantDept);
        })
        .isInstanceOf(BusinessException.class)
        .hasMessage("危化品申请必须填写用途说明");
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
        
        // 重置mock以便下次迭代
        reset(userClient, applicationMapper);
    }
    
    /**
     * 属性 12: 危化品申请缺少使用地点时应拒绝
     * 
     * **Validates: Requirements 6.3**
     * 
     * 当用户申请领用危化品时，如果缺少使用地点，系统应拒绝申请。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 12: 危化品申请必需字段验证")
    void hazardousApplicationShouldRejectWhenUsageLocationMissing(
            @ForAll @StringLength(min = 1, max = 500) String usagePurpose,
            @ForAll @IntRange(min = 1, max = 1000) int applyQuantity) {
        
        // 准备测试数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        
        // 创建危化品申请请求（缺少使用地点）
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose(usagePurpose);
        request.setUsageLocation(null); // 缺少使用地点
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal(applyQuantity));
        request.setItems(Collections.singletonList(item));
        
        // Mock安全资质检查
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // 执行并验证异常
        assertThatThrownBy(() -> {
            applicationService.createApplication(request, applicantId, applicantName, applicantDept);
        })
        .isInstanceOf(BusinessException.class)
        .hasMessage("危化品申请必须填写使用地点");
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
        
        // 重置mock以便下次迭代
        reset(userClient, applicationMapper);
    }
    
    /**
     * 属性 12: 危化品申请用途说明为空字符串时应拒绝
     * 
     * **Validates: Requirements 6.3**
     * 
     * 当用户申请领用危化品时，如果用途说明为空字符串或仅包含空格，系统应拒绝申请。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 12: 危化品申请必需字段验证")
    void hazardousApplicationShouldRejectWhenUsagePurposeEmpty(
            @ForAll @StringLength(min = 1, max = 200) String usageLocation,
            @ForAll @IntRange(min = 1, max = 1000) int applyQuantity,
            @ForAll @IntRange(min = 1, max = 10) int whitespaceCount) {
        
        // 准备测试数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        
        // 创建仅包含空格的用途说明
        String emptyUsagePurpose = " ".repeat(whitespaceCount);
        
        // 创建危化品申请请求（用途说明为空字符串）
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose(emptyUsagePurpose);
        request.setUsageLocation(usageLocation);
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal(applyQuantity));
        request.setItems(Collections.singletonList(item));
        
        // Mock安全资质检查
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // 执行并验证异常
        assertThatThrownBy(() -> {
            applicationService.createApplication(request, applicantId, applicantName, applicantDept);
        })
        .isInstanceOf(BusinessException.class)
        .hasMessage("危化品申请必须填写用途说明");
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
        
        // 重置mock以便下次迭代
        reset(userClient, applicationMapper);
    }
    
    /**
     * 属性 12: 危化品申请使用地点为空字符串时应拒绝
     * 
     * **Validates: Requirements 6.3**
     * 
     * 当用户申请领用危化品时，如果使用地点为空字符串或仅包含空格，系统应拒绝申请。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 12: 危化品申请必需字段验证")
    void hazardousApplicationShouldRejectWhenUsageLocationEmpty(
            @ForAll @StringLength(min = 1, max = 500) String usagePurpose,
            @ForAll @IntRange(min = 1, max = 1000) int applyQuantity,
            @ForAll @IntRange(min = 1, max = 10) int whitespaceCount) {
        
        // 准备测试数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        
        // 创建仅包含空格的使用地点
        String emptyUsageLocation = " ".repeat(whitespaceCount);
        
        // 创建危化品申请请求（使用地点为空字符串）
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(2); // 危化品领用
        request.setUsagePurpose(usagePurpose);
        request.setUsageLocation(emptyUsageLocation);
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal(applyQuantity));
        request.setItems(Collections.singletonList(item));
        
        // Mock安全资质检查
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // 执行并验证异常
        assertThatThrownBy(() -> {
            applicationService.createApplication(request, applicantId, applicantName, applicantDept);
        })
        .isInstanceOf(BusinessException.class)
        .hasMessage("危化品申请必须填写使用地点");
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
        
        // 重置mock以便下次迭代
        reset(userClient, applicationMapper);
    }
}
