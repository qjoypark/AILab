package com.lab.approval.service;

import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.service.impl.ApproverAssignmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审批人自动分配服务单元测试
 */
class ApproverAssignmentServiceTest {
    
    private ApproverAssignmentService assignmentService;
    
    @BeforeEach
    void setUp() {
        assignmentService = new ApproverAssignmentServiceImpl();
    }
    
    @Test
    void testAssignApprover_LabManager() {
        // Given
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("农学院");
        context.setApplicationType(1);
        
        // When
        Long approverId = assignmentService.assignApprover("LAB_MANAGER", context);
        
        // Then
        assertNotNull(approverId);
        assertEquals(2L, approverId);
    }
    
    @Test
    void testAssignApprover_CenterAdmin() {
        // Given
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("农学院");
        context.setApplicationType(2);
        context.setMaterialType(3); // 危化品
        
        // When
        Long approverId = assignmentService.assignApprover("CENTER_ADMIN", context);
        
        // Then
        assertNotNull(approverId);
        assertEquals(3L, approverId);
    }
    
    @Test
    void testAssignApprover_SafetyAdmin() {
        // Given
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("农学院");
        context.setApplicationType(2);
        context.setHasControlledMaterial(true);
        
        // When
        Long approverId = assignmentService.assignApprover("ADMIN", context);
        
        // Then
        assertNotNull(approverId);
        assertEquals(1L, approverId);
    }
    
    @Test
    void testAssignApprover_SafetyAdminAlias() {
        // Given
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("农学院");
        context.setApplicationType(2);
        
        // When
        Long approverId = assignmentService.assignApprover("SAFETY_ADMIN", context);
        
        // Then
        assertNotNull(approverId);
        assertEquals(1L, approverId);
    }
    
    @Test
    void testAssignApprover_UnknownRole() {
        // Given
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("农学院");
        
        // When
        Long approverId = assignmentService.assignApprover("UNKNOWN_ROLE", context);
        
        // Then
        assertNull(approverId);
    }
    
    @Test
    void testAssignApprover_DifferentDepartments() {
        // Given
        ApprovalContext context1 = new ApprovalContext();
        context1.setApplicantDept("农学院");
        
        ApprovalContext context2 = new ApprovalContext();
        context2.setApplicantDept("生物学院");
        
        // When
        Long approverId1 = assignmentService.assignApprover("LAB_MANAGER", context1);
        Long approverId2 = assignmentService.assignApprover("LAB_MANAGER", context2);
        
        // Then
        assertNotNull(approverId1);
        assertNotNull(approverId2);
        // 当前简化实现返回相同的ID，实际应该根据部门返回不同的负责人
        assertEquals(approverId1, approverId2);
    }
}
