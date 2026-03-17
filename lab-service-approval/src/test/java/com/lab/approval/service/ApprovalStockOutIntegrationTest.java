package com.lab.approval.service;

import com.lab.approval.client.InventoryClient;
import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApprovalProcessRequest;
import com.lab.approval.entity.ApprovalFlowConfig;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.entity.MaterialApplicationItem;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 审批通过后自动创建出库单集成测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("审批通过后自动创建出库单集成测试")
class ApprovalStockOutIntegrationTest {
    
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
    
    @InjectMocks
    private MaterialApplicationServiceImpl applicationService;
    
    private MaterialApplication mockApplication;
    private List<ApprovalRecord> mockApprovalRecords;
    
    @BeforeEach
    void setUp() {
        // 准备模拟申请单
        mockApplication = new MaterialApplication();
        mockApplication.setId(1L);
        mockApplication.setApplicationNo("APP20240101000001");
        mockApplication.setApplicantId(100L);
        mockApplication.setApplicantName("张三");
        mockApplication.setApplicantDept("实验室A");
        mockApplication.setApplicationType(1); // 普通领用
        mockApplication.setUsagePurpose("实验使用");
        mockApplication.setUsageLocation("实验室101");
        mockApplication.setStatus(2); // 审批中
        mockApplication.setApprovalStatus(1); // 审批中
        mockApplication.setCurrentApproverId(200L);
        mockApplication.setCreatedTime(LocalDateTime.now());
        
        // 准备审批记录（模拟已有一级审批）
        mockApprovalRecords = new ArrayList<>();
        ApprovalRecord record1 = new ApprovalRecord();
        record1.setId(1L);
        record1.setApplicationId(1L);
        record1.setApplicationNo("APP20240101000001");
        record1.setApproverId(200L);
        record1.setApproverName("审批人1");
        record1.setApprovalLevel(1);
        record1.setApprovalResult(1); // 通过
        record1.setApprovalOpinion("同意");
        record1.setApprovalTime(LocalDateTime.now());
        mockApprovalRecords.add(record1);
    }
    
    @Test
    @DisplayName("最后一级审批通过后应自动创建出库单")
    void shouldCreateStockOutWhenFinalApprovalPassed() {
        // Given
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1); // 通过
        request.setApprovalOpinion("同意出库");
        
        when(applicationMapper.selectById(1L)).thenReturn(mockApplication);
        when(approvalWorkflowService.getApprovalHistory(1L)).thenReturn(mockApprovalRecords);
        when(approvalRecordMapper.insert(any(ApprovalRecord.class))).thenReturn(1);
        when(flowConfigMapper.selectOne(any())).thenReturn(null); // 没有配置，默认单级审批
        when(applicationMapper.updateById(any(MaterialApplication.class))).thenReturn(1);
        
        // 模拟创建出库单成功
        when(inventoryClient.createStockOutOrder(1L)).thenReturn(1001L);
        
        // When
        applicationService.processApproval(1L, request, 200L, "审批人1");
        
        // Then
        // 验证调用了创建出库单接口
        verify(inventoryClient, times(1)).createStockOutOrder(1L);
        
        // 验证更新了申请单状态为审批通过
        verify(applicationMapper, times(1)).updateById(argThat(app -> 
            app.getStatus() == 3 && app.getApprovalStatus() == 2
        ));
    }
    
    @Test
    @DisplayName("非最后一级审批通过时不应创建出库单")
    void shouldNotCreateStockOutWhenNotFinalApproval() {
        // Given
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1); // 通过
        request.setApprovalOpinion("同意，流转下一级");
        
        // 模拟有多级审批流程
        ApprovalFlowConfig flowConfig = new ApprovalFlowConfig();
        flowConfig.setId(1L);
        flowConfig.setBusinessType(1);
        flowConfig.setFlowDefinition("{\"levels\":[{\"level\":1,\"name\":\"一级审批\"},{\"level\":2,\"name\":\"二级审批\"}]}");
        flowConfig.setStatus(1);
        
        when(applicationMapper.selectById(1L)).thenReturn(mockApplication);
        when(approvalWorkflowService.getApprovalHistory(1L)).thenReturn(new ArrayList<>()); // 第一级审批
        when(approvalRecordMapper.insert(any(ApprovalRecord.class))).thenReturn(1);
        when(flowConfigMapper.selectOne(any())).thenReturn(flowConfig);
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), any(), any())).thenReturn(201L);
        when(applicationMapper.updateById(any(MaterialApplication.class))).thenReturn(1);
        
        // When
        applicationService.processApproval(1L, request, 200L, "审批人1");
        
        // Then
        // 验证没有调用创建出库单接口
        verify(inventoryClient, never()).createStockOutOrder(anyLong());
        
        // 验证申请单仍然是审批中状态
        verify(applicationMapper, times(1)).updateById(argThat(app -> 
            app.getStatus() == 2 && app.getApprovalStatus() == 1
        ));
    }
    
    @Test
    @DisplayName("审批拒绝时不应创建出库单")
    void shouldNotCreateStockOutWhenApprovalRejected() {
        // Given
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(2); // 拒绝
        request.setApprovalOpinion("不同意");
        
        when(applicationMapper.selectById(1L)).thenReturn(mockApplication);
        when(approvalWorkflowService.getApprovalHistory(1L)).thenReturn(mockApprovalRecords);
        when(approvalRecordMapper.insert(any(ApprovalRecord.class))).thenReturn(1);
        when(applicationMapper.updateById(any(MaterialApplication.class))).thenReturn(1);
        
        // When
        applicationService.processApproval(1L, request, 200L, "审批人1");
        
        // Then
        // 验证没有调用创建出库单接口
        verify(inventoryClient, never()).createStockOutOrder(anyLong());
        
        // 验证申请单状态为审批拒绝
        verify(applicationMapper, times(1)).updateById(argThat(app -> 
            app.getStatus() == 4 && app.getApprovalStatus() == 3
        ));
    }
    
    @Test
    @DisplayName("创建出库单失败时不应影响审批流程")
    void shouldNotAffectApprovalWhenStockOutCreationFails() {
        // Given
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1); // 通过
        request.setApprovalOpinion("同意出库");
        
        when(applicationMapper.selectById(1L)).thenReturn(mockApplication);
        when(approvalWorkflowService.getApprovalHistory(1L)).thenReturn(mockApprovalRecords);
        when(approvalRecordMapper.insert(any(ApprovalRecord.class))).thenReturn(1);
        when(flowConfigMapper.selectOne(any())).thenReturn(null);
        when(applicationMapper.updateById(any(MaterialApplication.class))).thenReturn(1);
        
        // 模拟创建出库单失败
        when(inventoryClient.createStockOutOrder(1L)).thenReturn(null);
        
        // When
        applicationService.processApproval(1L, request, 200L, "审批人1");
        
        // Then
        // 验证调用了创建出库单接口
        verify(inventoryClient, times(1)).createStockOutOrder(1L);
        
        // 验证审批流程仍然完成，申请单状态为审批通过
        verify(applicationMapper, times(1)).updateById(argThat(app -> 
            app.getStatus() == 3 && app.getApprovalStatus() == 2
        ));
    }
    
    @Test
    @DisplayName("创建出库单异常时不应影响审批流程")
    void shouldNotAffectApprovalWhenStockOutCreationThrowsException() {
        // Given
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1); // 通过
        request.setApprovalOpinion("同意出库");
        
        when(applicationMapper.selectById(1L)).thenReturn(mockApplication);
        when(approvalWorkflowService.getApprovalHistory(1L)).thenReturn(mockApprovalRecords);
        when(approvalRecordMapper.insert(any(ApprovalRecord.class))).thenReturn(1);
        when(flowConfigMapper.selectOne(any())).thenReturn(null);
        when(applicationMapper.updateById(any(MaterialApplication.class))).thenReturn(1);
        
        // 模拟创建出库单抛出异常
        when(inventoryClient.createStockOutOrder(1L)).thenThrow(new RuntimeException("网络错误"));
        
        // When
        applicationService.processApproval(1L, request, 200L, "审批人1");
        
        // Then
        // 验证调用了创建出库单接口
        verify(inventoryClient, times(1)).createStockOutOrder(1L);
        
        // 验证审批流程仍然完成，申请单状态为审批通过
        verify(applicationMapper, times(1)).updateById(argThat(app -> 
            app.getStatus() == 3 && app.getApprovalStatus() == 2
        ));
    }
}
