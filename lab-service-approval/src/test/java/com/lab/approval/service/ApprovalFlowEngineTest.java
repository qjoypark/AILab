package com.lab.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApprovalFlowDefinition;
import com.lab.approval.dto.ApprovalRequest;
import com.lab.approval.entity.ApprovalFlowConfig;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.service.impl.ApprovalFlowEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 审批流程引擎单元测试
 */
@ExtendWith(MockitoExtension.class)
class ApprovalFlowEngineTest {
    
    @Mock
    private ApprovalFlowConfigMapper flowConfigMapper;
    
    @Mock
    private ApprovalRecordMapper recordMapper;
    
    @Mock
    private ApproverAssignmentService assignmentService;
    
    @InjectMocks
    private ApprovalFlowEngineImpl approvalFlowEngine;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // 使用反射设置objectMapper
        try {
            java.lang.reflect.Field field = ApprovalFlowEngineImpl.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(approvalFlowEngine, objectMapper);
        } catch (Exception e) {
            fail("Failed to inject objectMapper: " + e.getMessage());
        }
    }
    
    @Test
    void testParseFlowDefinition_Success() {
        // Given
        String json = "{\"levels\":[{\"level\":1,\"approverRole\":\"LAB_MANAGER\",\"approverName\":\"实验室负责人\"}]}";
        
        // When
        ApprovalFlowDefinition result = approvalFlowEngine.parseFlowDefinition(json);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getLevels());
        assertEquals(1, result.getLevels().size());
        assertEquals(1, result.getLevels().get(0).getLevel());
        assertEquals("LAB_MANAGER", result.getLevels().get(0).getApproverRole());
    }
    
    @Test
    void testParseFlowDefinition_MultiLevel() {
        // Given
        String json = "{\"levels\":[" +
                "{\"level\":1,\"approverRole\":\"LAB_MANAGER\",\"approverName\":\"实验室负责人\"}," +
                "{\"level\":2,\"approverRole\":\"CENTER_ADMIN\",\"approverName\":\"中心管理员\"}," +
                "{\"level\":3,\"approverRole\":\"ADMIN\",\"approverName\":\"安全管理员\"}" +
                "]}";
        
        // When
        ApprovalFlowDefinition result = approvalFlowEngine.parseFlowDefinition(json);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.getLevels().size());
        assertEquals("LAB_MANAGER", result.getLevels().get(0).getApproverRole());
        assertEquals("CENTER_ADMIN", result.getLevels().get(1).getApproverRole());
        assertEquals("ADMIN", result.getLevels().get(2).getApproverRole());
    }
    
    @Test
    void testStartApprovalFlow_Success() {
        // Given
        Long applicationId = 1L;
        String applicationNo = "APP001";
        ApprovalContext context = new ApprovalContext();
        context.setApplicationType(1); // 普通领用
        context.setApplicantDept("农学院");
        
        ApprovalFlowConfig flowConfig = new ApprovalFlowConfig();
        flowConfig.setFlowDefinition("{\"levels\":[{\"level\":1,\"approverRole\":\"LAB_MANAGER\",\"approverName\":\"实验室负责人\"}]}");
        
        when(flowConfigMapper.selectOne(any())).thenReturn(flowConfig);
        when(assignmentService.assignApprover(eq("LAB_MANAGER"), any())).thenReturn(2L);
        
        // When
        boolean result = approvalFlowEngine.startApprovalFlow(applicationId, applicationNo, context);
        
        // Then
        assertTrue(result);
        verify(flowConfigMapper, times(1)).selectOne(any());
        verify(assignmentService, times(1)).assignApprover(eq("LAB_MANAGER"), any());
    }
    
    @Test
    void testProcessApproval_Pass() {
        // Given
        ApprovalRequest request = new ApprovalRequest();
        request.setApplicationId(1L);
        request.setApprovalResult(1); // 通过
        request.setApprovalOpinion("同意");
        
        Long approverId = 2L;
        String approverName = "张三";
        
        when(recordMapper.selectOne(any())).thenReturn(null); // 第一级审批
        when(recordMapper.insert(any())).thenReturn(1);
        
        // When
        boolean result = approvalFlowEngine.processApproval(request, approverId, approverName);
        
        // Then
        assertTrue(result);
        verify(recordMapper, times(1)).insert(any(ApprovalRecord.class));
    }
    
    @Test
    void testProcessApproval_Reject() {
        // Given
        ApprovalRequest request = new ApprovalRequest();
        request.setApplicationId(1L);
        request.setApprovalResult(2); // 拒绝
        request.setApprovalOpinion("不符合要求");
        
        Long approverId = 2L;
        String approverName = "张三";
        
        when(recordMapper.selectOne(any())).thenReturn(null);
        when(recordMapper.insert(any())).thenReturn(1);
        
        // When
        boolean result = approvalFlowEngine.processApproval(request, approverId, approverName);
        
        // Then
        assertTrue(result);
        verify(recordMapper, times(1)).insert(any(ApprovalRecord.class));
    }
    
    @Test
    void testProcessApproval_Transfer() {
        // Given
        ApprovalRequest request = new ApprovalRequest();
        request.setApplicationId(1L);
        request.setApprovalResult(3); // 转审
        request.setApprovalOpinion("转给李四处理");
        request.setTransferToUserId(3L);
        
        Long approverId = 2L;
        String approverName = "张三";
        
        when(recordMapper.selectOne(any())).thenReturn(null);
        when(recordMapper.insert(any())).thenReturn(1);
        
        // When
        boolean result = approvalFlowEngine.processApproval(request, approverId, approverName);
        
        // Then
        assertTrue(result);
        verify(recordMapper, times(1)).insert(any(ApprovalRecord.class));
    }
    
    @Test
    void testGetCurrentApprovalLevel_FirstLevel() {
        // Given
        Long applicationId = 1L;
        when(recordMapper.selectOne(any())).thenReturn(null);
        
        // When
        Integer level = approvalFlowEngine.getCurrentApprovalLevel(applicationId);
        
        // Then
        assertEquals(1, level);
    }
    
    @Test
    void testGetCurrentApprovalLevel_SecondLevel() {
        // Given
        Long applicationId = 1L;
        ApprovalRecord record = new ApprovalRecord();
        record.setApprovalLevel(1);
        record.setApprovalResult(1); // 通过
        
        when(recordMapper.selectOne(any())).thenReturn(record);
        
        // When
        Integer level = approvalFlowEngine.getCurrentApprovalLevel(applicationId);
        
        // Then
        assertEquals(2, level);
    }
    
    @Test
    void testGetCurrentApprovalLevel_Rejected() {
        // Given
        Long applicationId = 1L;
        ApprovalRecord record = new ApprovalRecord();
        record.setApprovalLevel(1);
        record.setApprovalResult(2); // 拒绝
        
        when(recordMapper.selectOne(any())).thenReturn(record);
        
        // When
        Integer level = approvalFlowEngine.getCurrentApprovalLevel(applicationId);
        
        // Then
        assertNull(level);
    }
}
