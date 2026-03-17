package com.lab.approval.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.client.InventoryClient;
import com.lab.approval.client.MaterialClient;
import com.lab.approval.dto.CreateApplicationRequest;
import com.lab.approval.dto.MaterialApplicationDTO;
import com.lab.approval.dto.MaterialInfo;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.entity.MaterialApplicationItem;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.mapper.MaterialApplicationItemMapper;
import com.lab.approval.mapper.MaterialApplicationMapper;
import com.lab.approval.service.impl.MaterialApplicationServiceImpl;
import com.lab.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 领用申请服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class MaterialApplicationServiceTest {
    
    @Mock
    private MaterialApplicationMapper applicationMapper;
    
    @Mock
    private MaterialApplicationItemMapper itemMapper;
    
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    
    @Mock
    private ApprovalWorkflowService approvalWorkflowService;
    
    @Mock
    private InventoryClient inventoryClient;
    
    @Mock
    private MaterialClient materialClient;
    
    @InjectMocks
    private MaterialApplicationServiceImpl applicationService;
    
    private CreateApplicationRequest createValidRequest() {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicationType(1); // 普通领用
        request.setUsagePurpose("实验使用");
        request.setUsageLocation("实验室A");
        request.setExpectedDate(LocalDate.now().plusDays(1));
        
        CreateApplicationRequest.ApplicationItemRequest item = new CreateApplicationRequest.ApplicationItemRequest();
        item.setMaterialId(1L);
        item.setApplyQuantity(BigDecimal.valueOf(10));
        
        request.setItems(Collections.singletonList(item));
        
        return request;
    }
    
    private MaterialInfo createMaterialInfo(Long id) {
        MaterialInfo info = new MaterialInfo();
        info.setId(id);
        info.setMaterialName("测试药品");
        info.setSpecification("100ml");
        info.setUnit("瓶");
        info.setMaterialType(1);
        return info;
    }
    
    @Test
    void testCreateApplication_Success() {
        // Arrange
        CreateApplicationRequest request = createValidRequest();
        
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class)))
            .thenReturn(true);
        when(materialClient.getMaterialInfo(anyLong()))
            .thenReturn(createMaterialInfo(1L));
        when(applicationMapper.insert(any(MaterialApplication.class)))
            .thenAnswer(invocation -> {
                MaterialApplication app = invocation.getArgument(0);
                app.setId(1L);
                return 1;
            });
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), anyString(), any()))
            .thenReturn(100L);
        
        // Act
        Long applicationId = applicationService.createApplication(
            request, 1L, "测试用户", "测试部门"
        );
        
        // Assert
        assertNotNull(applicationId);
        verify(applicationMapper, times(2)).insert(any()); // insert + update
        verify(itemMapper, times(1)).insert(any());
        verify(approvalWorkflowService, times(1)).initializeApprovalWorkflow(anyLong(), anyString(), any());
    }
    
    @Test
    void testCreateApplication_EmptyItems_ThrowsException() {
        // Arrange
        CreateApplicationRequest request = createValidRequest();
        request.setItems(Collections.emptyList());
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(request, 1L, "测试用户", "测试部门");
        });
        
        assertEquals("申请明细不能为空", exception.getMessage());
    }
    
    @Test
    void testCreateApplication_InvalidQuantity_ThrowsException() {
        // Arrange
        CreateApplicationRequest request = createValidRequest();
        request.getItems().get(0).setApplyQuantity(BigDecimal.ZERO);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(request, 1L, "测试用户", "测试部门");
        });
        
        assertEquals("申请数量必须大于0", exception.getMessage());
    }
    
    @Test
    void testCreateApplication_InsufficientStock_ThrowsException() {
        // Arrange
        CreateApplicationRequest request = createValidRequest();
        
        when(inventoryClient.checkStockAvailability(anyLong(), any(BigDecimal.class)))
            .thenReturn(false);
        when(inventoryClient.getAvailableStock(anyLong()))
            .thenReturn(BigDecimal.valueOf(5));
        when(materialClient.getMaterialInfo(anyLong()))
            .thenReturn(createMaterialInfo(1L));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.createApplication(request, 1L, "测试用户", "测试部门");
        });
        
        assertTrue(exception.getMessage().contains("库存不足"));
    }
    
    @Test
    void testListApplications() {
        // Arrange
        Page<MaterialApplication> expectedPage = new Page<>(1, 10);
        when(applicationMapper.selectPage(any(), any()))
            .thenReturn(expectedPage);
        
        // Act
        Page<MaterialApplication> result = applicationService.listApplications(
            1, 10, null, null, null, null
        );
        
        // Assert
        assertNotNull(result);
        verify(applicationMapper, times(1)).selectPage(any(), any());
    }
    
    @Test
    void testGetApplicationDetail() {
        // Arrange
        MaterialApplication application = new MaterialApplication();
        application.setId(1L);
        application.setApplicationNo("APP20240101000001");
        application.setApplicantName("测试用户");
        
        when(applicationMapper.selectById(1L)).thenReturn(application);
        when(itemMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(approvalWorkflowService.getApprovalHistory(1L)).thenReturn(new ArrayList<>());
        
        // Act
        MaterialApplicationDTO result = applicationService.getApplicationDetail(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("APP20240101000001", result.getApplicationNo());
    }
    
    @Test
    void testGetApplicationDetail_NotFound_ThrowsException() {
        // Arrange
        when(applicationMapper.selectById(1L)).thenReturn(null);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.getApplicationDetail(1L);
        });
        
        assertEquals("申请单不存在", exception.getMessage());
    }
    
    @Test
    void testCancelApplication_Success() {
        // Arrange
        MaterialApplication application = new MaterialApplication();
        application.setId(1L);
        application.setApplicantId(1L);
        application.setStatus(2); // 审批中
        
        when(applicationMapper.selectById(1L)).thenReturn(application);
        when(applicationMapper.updateById(any())).thenReturn(1);
        
        // Act
        applicationService.cancelApplication(1L, 1L);
        
        // Assert
        verify(applicationMapper, times(1)).updateById(any());
    }
    
    @Test
    void testCancelApplication_NotApplicant_ThrowsException() {
        // Arrange
        MaterialApplication application = new MaterialApplication();
        application.setId(1L);
        application.setApplicantId(1L);
        application.setStatus(2);
        
        when(applicationMapper.selectById(1L)).thenReturn(application);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.cancelApplication(1L, 2L); // 不同的用户ID
        });
        
        assertEquals("只有申请人可以取消申请", exception.getMessage());
    }
    
    @Test
    void testCancelApplication_InvalidStatus_ThrowsException() {
        // Arrange
        MaterialApplication application = new MaterialApplication();
        application.setId(1L);
        application.setApplicantId(1L);
        application.setStatus(5); // 已出库
        
        when(applicationMapper.selectById(1L)).thenReturn(application);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            applicationService.cancelApplication(1L, 1L);
        });
        
        assertEquals("当前状态不允许取消", exception.getMessage());
    }
}
