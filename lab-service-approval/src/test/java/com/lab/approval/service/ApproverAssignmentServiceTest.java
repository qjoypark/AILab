package com.lab.approval.service;

import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApproverCandidateDTO;
import com.lab.approval.mapper.ApproverUserMapper;
import com.lab.approval.service.impl.ApproverAssignmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproverAssignmentServiceTest {

    @Mock
    private ApproverUserMapper approverUserMapper;

    private ApproverAssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new ApproverAssignmentServiceImpl(approverUserMapper);
    }

    @Test
    void testAssignApprover_LabManager() {
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("Agriculture College");
        context.setApplicationType(1);
        when(approverUserMapper.selectApproversByRoleCodes(anyList()))
            .thenReturn(List.of(approver(3L, "CENTER_ADMIN")));

        Long approverId = assignmentService.assignApprover("LAB_MANAGER", context);

        assertNotNull(approverId);
        assertEquals(3L, approverId);
    }

    @Test
    void testAssignApprover_CenterAdmin() {
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("Agriculture College");
        context.setApplicationType(2);
        context.setMaterialType(3);
        when(approverUserMapper.selectApproversByRoleCodes(anyList()))
            .thenReturn(List.of(approver(3L, "CENTER_ADMIN")));

        Long approverId = assignmentService.assignApprover("CENTER_ADMIN", context);

        assertNotNull(approverId);
        assertEquals(3L, approverId);
    }

    @Test
    void testAssignApprover_SafetyAdminUsesTemporaryCenterAdminPolicy() {
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("Agriculture College");
        context.setApplicationType(2);
        context.setHasControlledMaterial(true);
        when(approverUserMapper.selectApproversByRoleCodes(anyList()))
            .thenReturn(List.of(approver(3L, "CENTER_ADMIN")));

        Long approverId = assignmentService.assignApprover("ADMIN", context);

        assertNotNull(approverId);
        assertEquals(3L, approverId);
    }

    @Test
    void testAssignApprover_SafetyAdminAliasUsesTemporaryCenterAdminPolicy() {
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("Agriculture College");
        context.setApplicationType(2);
        when(approverUserMapper.selectApproversByRoleCodes(anyList()))
            .thenReturn(List.of(approver(3L, "CENTER_ADMIN")));

        Long approverId = assignmentService.assignApprover("SAFETY_ADMIN", context);

        assertNotNull(approverId);
        assertEquals(3L, approverId);
    }

    @Test
    void testAssignApprover_UnknownRole() {
        ApprovalContext context = new ApprovalContext();
        context.setApplicantDept("Agriculture College");
        when(approverUserMapper.selectApproversByRoleCodes(anyList()))
            .thenReturn(List.of());

        Long approverId = assignmentService.assignApprover("UNKNOWN_ROLE", context);

        assertNull(approverId);
    }

    @Test
    void testAssignApprover_DifferentDepartments() {
        ApprovalContext context1 = new ApprovalContext();
        context1.setApplicantDept("Agriculture College");

        ApprovalContext context2 = new ApprovalContext();
        context2.setApplicantDept("Biology College");

        when(approverUserMapper.selectApproversByRoleCodes(anyList()))
            .thenReturn(List.of(approver(2L, "LAB_MANAGER")));

        Long approverId1 = assignmentService.assignApprover("LAB_MANAGER", context1);
        Long approverId2 = assignmentService.assignApprover("LAB_MANAGER", context2);

        assertNotNull(approverId1);
        assertNotNull(approverId2);
        assertEquals(approverId1, approverId2);
    }

    private ApproverCandidateDTO approver(Long userId, String roleCode) {
        ApproverCandidateDTO approver = new ApproverCandidateDTO();
        approver.setUserId(userId);
        approver.setRoleCode(roleCode);
        return approver;
    }
}
