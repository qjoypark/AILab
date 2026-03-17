package com.lab.approval.service;

import com.lab.approval.client.InventoryClient;
import com.lab.approval.client.MaterialClient;
import com.lab.approval.client.UserClient;
import com.lab.approval.dto.CreateApplicationRequest;
import com.lab.approval.dto.MaterialInfo;
import com.lab.approval.dto.UserInfo;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.mapper.MaterialApplicationItemMapper;
import com.lab.approval.mapper.MaterialApplicationMapper;
import com.lab.approval.service.impl.MaterialApplicationServiceImpl;
import com.lab.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 危化品申请验证测试
 * 验证需求 6.3, 6.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("危化品申请验证测试")
class HazardousApplicationValidationTest {
    
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
    
    private CreateApplicationRequest hazardousRequest;
    private Long applicantId;
    private String applicantName;
    private String applicantDept;
    
    @BeforeEach
    void setUp() {
        applicantId = 1L;
        applicantName = "测试用户";
        applicantDept = "测试部门";
        
        // 创建危化品申请请求
        hazardousRequest = new CreateApplicationRequest();
        hazardousRequest.setApplicationType(2); // 危化品领用
        hazardousRequest.setUsagePurpose("实验用途");
        hazardousRequest.setUsageLocation("实验室A101");
        hazardousRequest.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal("10"));
        hazardousRequest.setItems(Collections.singletonList(item));
        
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
        
        // Mock审批流程
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), anyString(), any()))
            .thenReturn(100L);
    }
    
    @Test
    @DisplayName("测试1: 安全资质未通过时拒绝危化品申请")
    void testRejectApplicationWhenSafetyCertNotPassed() {
        // 模拟安全资质未通过
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(false);
        
        // 执行并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(hazardousRequest, applicantId, applicantName, applicantDept);
        });
        
        assertEquals("安全资质未通过或已过期，无法申请危化品", exception.getMessage());
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
    }
    
    @Test
    @DisplayName("测试2: 安全资质已过期时拒绝危化品申请")
    void testRejectApplicationWhenSafetyCertExpired() {
        // 模拟安全资质已过期
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(false);
        
        // 执行并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(hazardousRequest, applicantId, applicantName, applicantDept);
        });
        
        assertEquals("安全资质未通过或已过期，无法申请危化品", exception.getMessage());
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
    }
    
    @Test
    @DisplayName("测试3: 危化品申请缺少用途说明时拒绝")
    void testRejectApplicationWhenUsagePurposeMissing() {
        // 模拟安全资质有效
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // 设置用途说明为空
        hazardousRequest.setUsagePurpose(null);
        
        // 执行并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(hazardousRequest, applicantId, applicantName, applicantDept);
        });
        
        assertEquals("危化品申请必须填写用途说明", exception.getMessage());
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
    }
    
    @Test
    @DisplayName("测试4: 危化品申请用途说明为空字符串时拒绝")
    void testRejectApplicationWhenUsagePurposeEmpty() {
        // 模拟安全资质有效
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // 设置用途说明为空字符串
        hazardousRequest.setUsagePurpose("   ");
        
        // 执行并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(hazardousRequest, applicantId, applicantName, applicantDept);
        });
        
        assertEquals("危化品申请必须填写用途说明", exception.getMessage());
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
    }
    
    @Test
    @DisplayName("测试5: 危化品申请缺少使用地点时拒绝")
    void testRejectApplicationWhenUsageLocationMissing() {
        // 模拟安全资质有效
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // 设置使用地点为空
        hazardousRequest.setUsageLocation(null);
        
        // 执行并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(hazardousRequest, applicantId, applicantName, applicantDept);
        });
        
        assertEquals("危化品申请必须填写使用地点", exception.getMessage());
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
    }
    
    @Test
    @DisplayName("测试6: 危化品申请使用地点为空字符串时拒绝")
    void testRejectApplicationWhenUsageLocationEmpty() {
        // 模拟安全资质有效
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // 设置使用地点为空字符串
        hazardousRequest.setUsageLocation("   ");
        
        // 执行并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(hazardousRequest, applicantId, applicantName, applicantDept);
        });
        
        assertEquals("危化品申请必须填写使用地点", exception.getMessage());
        
        // 验证没有创建申请单
        verify(applicationMapper, never()).insert(any(MaterialApplication.class));
    }
    
    @Test
    @DisplayName("测试7: 安全资质有效且必填字段完整时允许创建危化品申请")
    void testAllowApplicationWhenSafetyCertValidAndFieldsComplete() {
        // 模拟安全资质有效
        when(userClient.checkSafetyCertification(applicantId)).thenReturn(true);
        
        // Mock insert操作
        doAnswer(invocation -> {
            MaterialApplication app = invocation.getArgument(0);
            app.setId(1L);
            return 1;
        }).when(applicationMapper).insert(any(MaterialApplication.class));
        
        // 执行创建申请
        Long applicationId = applicationService.createApplication(
            hazardousRequest, applicantId, applicantName, applicantDept
        );
        
        // 验证申请创建成功
        assertNotNull(applicationId);
        
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
    }
    
    @Test
    @DisplayName("测试8: 普通申请不需要安全资质验证")
    void testNormalApplicationDoesNotRequireSafetyCert() {
        // 创建普通申请
        CreateApplicationRequest normalRequest = new CreateApplicationRequest();
        normalRequest.setApplicationType(1); // 普通领用
        normalRequest.setUsagePurpose("普通用途");
        normalRequest.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal("5"));
        normalRequest.setItems(Collections.singletonList(item));
        
        // Mock insert操作
        doAnswer(invocation -> {
            MaterialApplication app = invocation.getArgument(0);
            app.setId(2L);
            return 1;
        }).when(applicationMapper).insert(any(MaterialApplication.class));
        
        // 执行创建申请
        Long applicationId = applicationService.createApplication(
            normalRequest, applicantId, applicantName, applicantDept
        );
        
        // 验证申请创建成功
        assertNotNull(applicationId);
        
        // 验证没有调用安全资质检查
        verify(userClient, never()).checkSafetyCertification(anyLong());
        
        // 验证创建了申请单
        verify(applicationMapper, times(1)).insert(any(MaterialApplication.class));
    }
    
    @Test
    @DisplayName("测试9: 普通申请可以不填写使用地点")
    void testNormalApplicationCanOmitUsageLocation() {
        // 创建普通申请，不填写使用地点
        CreateApplicationRequest normalRequest = new CreateApplicationRequest();
        normalRequest.setApplicationType(1); // 普通领用
        normalRequest.setUsagePurpose("普通用途");
        normalRequest.setUsageLocation(null); // 不填写使用地点
        normalRequest.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(new BigDecimal("5"));
        normalRequest.setItems(Collections.singletonList(item));
        
        // Mock insert操作
        doAnswer(invocation -> {
            MaterialApplication app = invocation.getArgument(0);
            app.setId(3L);
            return 1;
        }).when(applicationMapper).insert(any(MaterialApplication.class));
        
        // 执行创建申请，不应该抛出异常
        assertDoesNotThrow(() -> {
            applicationService.createApplication(normalRequest, applicantId, applicantName, applicantDept);
        });
        
        // 验证创建了申请单
        verify(applicationMapper, times(1)).insert(any(MaterialApplication.class));
    }
}
