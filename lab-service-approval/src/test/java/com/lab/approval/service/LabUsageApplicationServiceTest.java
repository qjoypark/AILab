package com.lab.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.approval.client.InventoryClient;
import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApprovalProcessRequest;
import com.lab.approval.dto.ApproverCandidateDTO;
import com.lab.approval.dto.CreateLabUsageApplicationRequest;
import com.lab.approval.dto.LabUsageApplicationDTO;
import com.lab.approval.dto.LabUsageParticipantDTO;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.entity.LabRoom;
import com.lab.approval.entity.LabUsageApplication;
import com.lab.approval.entity.LabUsageParticipant;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.mapper.LabRoomManagerMapper;
import com.lab.approval.mapper.LabRoomMapper;
import com.lab.approval.mapper.LabUsageApplicationMapper;
import com.lab.approval.mapper.LabUsageParticipantMapper;
import com.lab.approval.service.impl.LabUsageApplicationServiceImpl;
import com.lab.approval.support.RequestUserContextResolver;
import com.lab.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabUsageApplicationServiceTest {

    @Mock
    private LabUsageApplicationMapper applicationMapper;

    @Mock
    private LabUsageParticipantMapper participantMapper;

    @Mock
    private LabRoomMapper labRoomMapper;

    @Mock
    private LabRoomManagerMapper labRoomManagerMapper;

    @Mock
    private ApprovalRecordMapper approvalRecordMapper;

    @Mock
    private ApprovalFlowConfigMapper approvalFlowConfigMapper;

    @Mock
    private ApprovalWorkflowService approvalWorkflowService;

    @Mock
    private ApproverAssignmentService approverAssignmentService;

    @Mock
    private InventoryClient inventoryClient;

    private LabUsageApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LabUsageApplicationServiceImpl(
                applicationMapper,
                participantMapper,
                labRoomMapper,
                labRoomManagerMapper,
                approvalRecordMapper,
                approvalFlowConfigMapper,
                approvalWorkflowService,
                approverAssignmentService,
                inventoryClient,
                new ObjectMapper()
        );
    }

    @Test
    void createApplication_rejectsDisabledLabRoom() {
        when(labRoomMapper.selectById(1L)).thenReturn(disabledRoom());

        assertThrows(BusinessException.class, () -> service.createApplication(validRequest(), teacher()));

        verify(labRoomManagerMapper, never()).selectCount(any());
        verify(applicationMapper, never()).insert(any(LabUsageApplication.class));
        verify(approvalWorkflowService, never()).initializeApprovalWorkflow(anyLong(), anyString(), any());
    }

    @Test
    void createApplication_rejectsLabRoomWithoutManager() {
        when(labRoomMapper.selectById(1L)).thenReturn(enabledRoom());
        when(labRoomManagerMapper.selectCount(any())).thenReturn(0L);

        assertThrows(BusinessException.class, () -> service.createApplication(validRequest(), teacher()));

        verify(applicationMapper, never()).insert(any(LabUsageApplication.class));
        verify(approvalWorkflowService, never()).initializeApprovalWorkflow(anyLong(), anyString(), any());
    }

    @Test
    void createApplication_rejectsStartTimeAfterEndTime() {
        CreateLabUsageApplicationRequest request = validRequest();
        request.setStartTime(LocalDateTime.of(2026, 5, 3, 12, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 3, 11, 0));

        assertThrows(BusinessException.class, () -> service.createApplication(request, teacher()));

        verify(labRoomMapper, never()).selectById(anyLong());
        verify(applicationMapper, never()).insert(any(LabUsageApplication.class));
        verify(approvalWorkflowService, never()).initializeApprovalWorkflow(anyLong(), anyString(), any());
    }

    @Test
    void createApplication_allowsSameRoomSameTimeApplications() {
        AtomicLong idSequence = new AtomicLong(100L);
        when(labRoomMapper.selectById(1L)).thenReturn(enabledRoom());
        when(labRoomManagerMapper.selectCount(any())).thenReturn(1L);
        when(applicationMapper.insert(any(LabUsageApplication.class))).thenAnswer(invocation -> {
            LabUsageApplication application = invocation.getArgument(0);
            application.setId(idSequence.getAndIncrement());
            return 1;
        });
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), anyString(), any())).thenReturn(15L);
        when(approvalFlowConfigMapper.selectOne(any())).thenReturn(null);
        when(approverAssignmentService.resolveEffectiveRoleCode(anyString(), any())).thenReturn("LAB_ROOM_MANAGER");

        service.createApplication(validRequest(), teacher());
        service.createApplication(validRequest(), teacher());

        ArgumentCaptor<LabUsageApplication> captor = ArgumentCaptor.forClass(LabUsageApplication.class);
        verify(applicationMapper, times(2)).insert(captor.capture());
        List<LabUsageApplication> applications = captor.getAllValues();
        assertEquals(applications.get(0).getLabRoomId(), applications.get(1).getLabRoomId());
        assertEquals(applications.get(0).getStartTime(), applications.get(1).getStartTime());
        assertEquals(applications.get(0).getEndTime(), applications.get(1).getEndTime());
        verify(approvalWorkflowService, times(2)).initializeApprovalWorkflow(anyLong(), anyString(), any());
    }

    @Test
    void createApplication_rejectsParticipantThatIsNotActiveSystemUser() {
        CreateLabUsageApplicationRequest request = validRequest();
        CreateLabUsageApplicationRequest.ParticipantRequest participant =
                new CreateLabUsageApplicationRequest.ParticipantRequest();
        participant.setUserId(999L);
        participant.setRealName("Fake Teacher");
        request.setParticipants(List.of(participant));

        when(labRoomMapper.selectById(1L)).thenReturn(enabledRoom());
        when(labRoomManagerMapper.selectCount(any())).thenReturn(1L);
        when(applicationMapper.insert(any(LabUsageApplication.class))).thenAnswer(invocation -> {
            LabUsageApplication application = invocation.getArgument(0);
            application.setId(200L);
            return 1;
        });
        when(participantMapper.selectActiveUsersByIds(List.of(999L))).thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> service.createApplication(request, teacher()));

        ArgumentCaptor<LabUsageParticipant> participantCaptor = ArgumentCaptor.forClass(LabUsageParticipant.class);
        verify(participantMapper, times(1)).insert(participantCaptor.capture());
        assertEquals(7L, participantCaptor.getValue().getUserId());
        verify(approvalWorkflowService, never()).initializeApprovalWorkflow(anyLong(), anyString(), any());
    }

    @Test
    void createApplication_usesSystemUserSnapshotForParticipants() {
        CreateLabUsageApplicationRequest request = validRequest();
        CreateLabUsageApplicationRequest.ParticipantRequest participant =
                new CreateLabUsageApplicationRequest.ParticipantRequest();
        participant.setUserId(20L);
        participant.setRealName("Client Supplied Name");
        participant.setDeptName("Client Supplied Dept");
        request.setParticipants(List.of(participant));

        LabUsageParticipantDTO activeUser = new LabUsageParticipantDTO();
        activeUser.setUserId(20L);
        activeUser.setRealName("System Teacher");
        activeUser.setDeptName("System Dept");

        when(labRoomMapper.selectById(1L)).thenReturn(enabledRoom());
        when(labRoomManagerMapper.selectCount(any())).thenReturn(1L);
        when(applicationMapper.insert(any(LabUsageApplication.class))).thenAnswer(invocation -> {
            LabUsageApplication application = invocation.getArgument(0);
            application.setId(300L);
            return 1;
        });
        when(participantMapper.selectActiveUsersByIds(List.of(20L))).thenReturn(List.of(activeUser));
        when(approvalWorkflowService.initializeApprovalWorkflow(anyLong(), anyString(), any())).thenReturn(15L);
        when(approvalFlowConfigMapper.selectOne(any())).thenReturn(null);
        when(approverAssignmentService.resolveEffectiveRoleCode(anyString(), any())).thenReturn("LAB_ROOM_MANAGER");

        service.createApplication(request, teacher());

        ArgumentCaptor<LabUsageParticipant> captor = ArgumentCaptor.forClass(LabUsageParticipant.class);
        verify(participantMapper, times(2)).insert(captor.capture());
        LabUsageParticipant selectedParticipant = captor.getAllValues().stream()
                .filter(item -> item.getUserId().equals(20L))
                .findFirst()
                .orElseThrow();
        assertEquals("System Teacher", selectedParticipant.getRealName());
        assertEquals("System Dept", selectedParticipant.getDeptName());
    }

    @Test
    void listPendingApplications_returnsOnlyApplicationsCurrentUserCanApprove() {
        LabUsageApplication managedByCurrentUser = approvingApplication();
        managedByCurrentUser.setId(10L);
        managedByCurrentUser.setLabRoomId(1L);
        managedByCurrentUser.setCurrentApproverId(15L);

        LabUsageApplication managedByOtherUser = approvingApplication();
        managedByOtherUser.setId(11L);
        managedByOtherUser.setLabRoomId(2L);
        managedByOtherUser.setCurrentApproverId(16L);

        when(applicationMapper.selectList(any())).thenReturn(List.of(managedByCurrentUser, managedByOtherUser));
        when(approvalWorkflowService.getApprovalHistory(3, 10L)).thenReturn(Collections.emptyList());
        when(approvalWorkflowService.getApprovalHistory(3, 11L)).thenReturn(Collections.emptyList());
        when(approvalFlowConfigMapper.selectOne(any())).thenReturn(null);
        when(approverAssignmentService.resolveEffectiveRoleCode(eq("LAB_ROOM_MANAGER"), any()))
                .thenReturn("LAB_ROOM_MANAGER");
        when(approverAssignmentService.listApproverCandidates(eq("LAB_ROOM_MANAGER"), any()))
                .thenAnswer(invocation -> {
                    ApprovalContext context = invocation.getArgument(1);
                    ApproverCandidateDTO candidate = new ApproverCandidateDTO();
                    candidate.setUserId(context.getLabRoomId().equals(1L) ? 15L : 16L);
                    candidate.setRealName(context.getLabRoomId().equals(1L) ? "Current Manager" : "Other Manager");
                    return List.of(candidate);
                });
        when(participantMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<LabUsageApplicationDTO> result = service.listPendingApplications(labManagerApprover());

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void getApplicationDetail_applicantCanSeeApprovalRecordsAndOpinions() {
        LabUsageApplication application = approvedApplication();
        ApprovalRecord record = approvalRecord("Manager pass");

        when(applicationMapper.selectById(10L)).thenReturn(application);
        when(participantMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(approvalWorkflowService.getApprovalHistory(3, 10L)).thenReturn(List.of(record));

        LabUsageApplicationDTO result = service.getApplicationDetail(10L, teacher());

        assertEquals(1, result.getApprovalRecords().size());
        assertEquals("Manager pass", result.getApprovalRecords().get(0).getApprovalOpinion());
    }

    @Test
    void getApplicationDetail_participantCanSeeApprovalRecordsAndOpinions() {
        LabUsageApplication application = approvedApplication();
        application.setApplicantId(7L);
        ApprovalRecord record = approvalRecord("Center director pass");
        LabUsageParticipant participant = participantApplication(10L, 20L);

        when(applicationMapper.selectById(10L)).thenReturn(application);
        when(labRoomManagerMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(participantMapper.selectList(any())).thenReturn(List.of(participant));
        when(approvalWorkflowService.getApprovalHistory(3, 10L)).thenReturn(List.of(record));

        LabUsageApplicationDTO result = service.getApplicationDetail(10L, participantTeacher());

        assertEquals(1, result.getApprovalRecords().size());
        assertEquals("Center director pass", result.getApprovalRecords().get(0).getApprovalOpinion());
    }

    @Test
    void getApplicationDetail_rejectsUnrelatedTeacher() {
        LabUsageApplication application = approvedApplication();
        application.setApplicantId(7L);

        when(applicationMapper.selectById(10L)).thenReturn(application);
        when(labRoomManagerMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(participantMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> service.getApplicationDetail(10L, participantTeacher()));

        verify(approvalWorkflowService, never()).getApprovalHistory(anyInt(), anyLong());
    }

    @Test
    void processApproval_rejectsUserOutsideCurrentApproverCandidates() {
        LabUsageApplication application = approvingApplication();
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1);
        request.setApprovalOpinion("OK");

        ApproverCandidateDTO manager = new ApproverCandidateDTO();
        manager.setUserId(15L);
        manager.setRealName("Lab Manager");

        when(applicationMapper.selectById(10L)).thenReturn(application);
        when(approvalWorkflowService.getApprovalHistory(3, 10L)).thenReturn(Collections.emptyList());
        when(approvalFlowConfigMapper.selectOne(any())).thenReturn(null);
        when(approverAssignmentService.resolveEffectiveRoleCode(eq("LAB_ROOM_MANAGER"), any())).thenReturn("LAB_ROOM_MANAGER");
        when(approverAssignmentService.listApproverCandidates(eq("LAB_ROOM_MANAGER"), any())).thenReturn(List.of(manager));

        assertThrows(BusinessException.class, () -> service.processApproval(10L, request, unauthorizedApprover()));

        verify(approvalRecordMapper, never()).insert(any(ApprovalRecord.class));
        verify(applicationMapper, never()).updateById(any(LabUsageApplication.class));
    }

    @Test
    void processApproval_advancesToCenterDirectorAfterLabManagerApproval() {
        LabUsageApplication application = approvingApplication();
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1);
        request.setApprovalOpinion("Manager pass");

        ApproverCandidateDTO manager = new ApproverCandidateDTO();
        manager.setUserId(15L);
        manager.setRealName("Current Manager");

        when(applicationMapper.selectById(10L)).thenReturn(application);
        when(approvalWorkflowService.getApprovalHistory(3, 10L)).thenReturn(Collections.emptyList());
        when(approvalFlowConfigMapper.selectOne(any())).thenReturn(null);
        when(approverAssignmentService.resolveEffectiveRoleCode(anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(approverAssignmentService.listApproverCandidates(eq("LAB_ROOM_MANAGER"), any())).thenReturn(List.of(manager));
        when(approverAssignmentService.assignApprover(eq("CENTER_DIRECTOR"), any())).thenReturn(31L);

        service.processApproval(10L, request, labManagerApprover());

        ArgumentCaptor<LabUsageApplication> captor = ArgumentCaptor.forClass(LabUsageApplication.class);
        verify(applicationMapper).updateById(captor.capture());
        LabUsageApplication updated = captor.getValue();
        assertEquals(2, updated.getStatus());
        assertEquals(1, updated.getApprovalStatus());
        assertEquals(31L, updated.getCurrentApproverId());
        assertEquals("CENTER_DIRECTOR", updated.getCurrentApproverRole());
        verify(approvalRecordMapper).insert(any(ApprovalRecord.class));
    }

    @Test
    void processApproval_rejectsAndTerminatesFlow() {
        LabUsageApplication application = approvingApplication();
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(2);
        request.setApprovalOpinion("Need adjustment");

        ApproverCandidateDTO manager = new ApproverCandidateDTO();
        manager.setUserId(15L);
        manager.setRealName("Current Manager");

        when(applicationMapper.selectById(10L)).thenReturn(application);
        when(approvalWorkflowService.getApprovalHistory(3, 10L)).thenReturn(Collections.emptyList());
        when(approvalFlowConfigMapper.selectOne(any())).thenReturn(null);
        when(approverAssignmentService.resolveEffectiveRoleCode(eq("LAB_ROOM_MANAGER"), any()))
                .thenReturn("LAB_ROOM_MANAGER");
        when(approverAssignmentService.listApproverCandidates(eq("LAB_ROOM_MANAGER"), any())).thenReturn(List.of(manager));

        service.processApproval(10L, request, labManagerApprover());

        ArgumentCaptor<LabUsageApplication> captor = ArgumentCaptor.forClass(LabUsageApplication.class);
        verify(applicationMapper).updateById(captor.capture());
        LabUsageApplication updated = captor.getValue();
        assertEquals(4, updated.getStatus());
        assertEquals(3, updated.getApprovalStatus());
        assertNull(updated.getCurrentApproverId());
        assertNull(updated.getCurrentApproverRole());
        verify(approvalRecordMapper).insert(any(ApprovalRecord.class));
    }

    @Test
    void processApproval_marksApplicationApprovedAfterDeanApproval() {
        LabUsageApplication application = approvingApplication();
        application.setCurrentApproverId(41L);
        application.setCurrentApproverRole("DEAN");
        ApprovalProcessRequest request = new ApprovalProcessRequest();
        request.setApprovalResult(1);
        request.setApprovalOpinion("Final pass");

        ApproverCandidateDTO dean = new ApproverCandidateDTO();
        dean.setUserId(41L);
        dean.setRealName("Dean");

        when(applicationMapper.selectById(10L)).thenReturn(application);
        when(approvalWorkflowService.getApprovalHistory(3, 10L)).thenReturn(
                List.of(new ApprovalRecord(), new ApprovalRecord(), new ApprovalRecord())
        );
        when(approvalFlowConfigMapper.selectOne(any())).thenReturn(null);
        when(approverAssignmentService.resolveEffectiveRoleCode(anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(approverAssignmentService.listApproverCandidates(eq("DEAN"), any())).thenReturn(List.of(dean));

        service.processApproval(10L, request, deanApprover());

        ArgumentCaptor<LabUsageApplication> captor = ArgumentCaptor.forClass(LabUsageApplication.class);
        verify(applicationMapper).updateById(captor.capture());
        LabUsageApplication updated = captor.getValue();
        assertEquals(3, updated.getStatus());
        assertEquals(2, updated.getApprovalStatus());
        assertNull(updated.getCurrentApproverId());
        assertNull(updated.getCurrentApproverRole());
        verify(approvalRecordMapper).insert(any(ApprovalRecord.class));
    }

    @Test
    void listOverlapApplications_returnsApprovingAndApprovedApplications() {
        LabUsageApplication approving = overlapApplication(
                501L,
                "LAB202605030501",
                2,
                LocalDateTime.of(2026, 5, 3, 8, 30),
                LocalDateTime.of(2026, 5, 3, 10, 0)
        );
        LabUsageApplication approved = overlapApplication(
                502L,
                "LAB202605030502",
                3,
                LocalDateTime.of(2026, 5, 3, 10, 0),
                LocalDateTime.of(2026, 5, 3, 12, 0)
        );
        when(applicationMapper.selectList(any())).thenReturn(List.of(approving, approved));
        when(participantMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<LabUsageApplicationDTO> result = service.listOverlapApplications(
                1L,
                LocalDateTime.of(2026, 5, 3, 9, 0),
                LocalDateTime.of(2026, 5, 3, 11, 0),
                teacher()
        );

        assertEquals(2, result.size());
        assertEquals(List.of(2, 3), result.stream().map(LabUsageApplicationDTO::getStatus).toList());
        assertEquals(List.of("LAB202605030501", "LAB202605030502"),
                result.stream().map(LabUsageApplicationDTO::getApplicationNo).toList());
    }

    private CreateLabUsageApplicationRequest validRequest() {
        CreateLabUsageApplicationRequest request = new CreateLabUsageApplicationRequest();
        request.setLabRoomId(1L);
        request.setUsageType(1);
        request.setUsagePurpose("Teaching lab");
        request.setProjectName("Biology 101");
        request.setExpectedAttendeeCount(30);
        request.setStartTime(LocalDateTime.of(2026, 5, 3, 9, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 3, 11, 0));
        request.setSafetyCommitment(1);
        request.setParticipants(Collections.emptyList());
        return request;
    }

    private LabRoom enabledRoom() {
        LabRoom room = new LabRoom();
        room.setId(1L);
        room.setRoomCode("A101");
        room.setRoomName("Shared Lab");
        room.setStatus(1);
        return room;
    }

    private LabRoom disabledRoom() {
        LabRoom room = enabledRoom();
        room.setStatus(0);
        return room;
    }

    private LabUsageApplication approvingApplication() {
        LabUsageApplication application = new LabUsageApplication();
        application.setId(10L);
        application.setApplicationNo("LAB202605030001");
        application.setApplicantId(7L);
        application.setApplicantName("Applicant");
        application.setLabRoomId(1L);
        application.setLabRoomCode("A101");
        application.setLabRoomName("Shared Lab");
        application.setUsagePurpose("Teaching lab");
        application.setStartTime(LocalDateTime.of(2026, 5, 3, 9, 0));
        application.setEndTime(LocalDateTime.of(2026, 5, 3, 11, 0));
        application.setStatus(2);
        application.setApprovalStatus(1);
        application.setCurrentApproverId(15L);
        application.setCurrentApproverRole("LAB_ROOM_MANAGER");
        return application;
    }

    private LabUsageApplication approvedApplication() {
        LabUsageApplication application = approvingApplication();
        application.setStatus(3);
        application.setApprovalStatus(2);
        application.setCurrentApproverId(null);
        application.setCurrentApproverRole(null);
        return application;
    }

    private ApprovalRecord approvalRecord(String opinion) {
        ApprovalRecord record = new ApprovalRecord();
        record.setId(100L);
        record.setApproverName("Approver");
        record.setApprovalLevel(1);
        record.setApprovalResult(1);
        record.setApprovalOpinion(opinion);
        record.setApprovalTime(LocalDateTime.of(2026, 5, 3, 12, 0));
        return record;
    }

    private LabUsageParticipant participantApplication(Long applicationId, Long userId) {
        LabUsageParticipant participant = new LabUsageParticipant();
        participant.setId(200L);
        participant.setApplicationId(applicationId);
        participant.setUserId(userId);
        participant.setRealName("Participant Teacher");
        participant.setDeptName("Biology");
        return participant;
    }

    private LabUsageApplication overlapApplication(Long id,
                                                   String applicationNo,
                                                   Integer status,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        LabUsageApplication application = new LabUsageApplication();
        application.setId(id);
        application.setApplicationNo(applicationNo);
        application.setApplicantId(7L);
        application.setApplicantName("Teacher Seven");
        application.setLabRoomId(1L);
        application.setLabRoomCode("A101");
        application.setLabRoomName("Shared Lab");
        application.setUsagePurpose("Teaching lab");
        application.setProjectName("Biology 101");
        application.setStartTime(startTime);
        application.setEndTime(endTime);
        application.setStatus(status);
        application.setApprovalStatus(status == 3 ? 2 : 1);
        return application;
    }

    private RequestUserContextResolver.CurrentUser teacher() {
        return new RequestUserContextResolver.CurrentUser(
                7L,
                "teacher7",
                "Teacher Seven",
                "Biology",
                List.of("TEACHER"),
                List.of("lab-usage:create", "lab-usage:list", "lab-usage:schedule:view")
        );
    }

    private RequestUserContextResolver.CurrentUser participantTeacher() {
        return new RequestUserContextResolver.CurrentUser(
                20L,
                "teacher20",
                "Teacher Twenty",
                "Biology",
                List.of("TEACHER"),
                List.of("lab-usage:create", "lab-usage:list", "lab-usage:schedule:view")
        );
    }

    private RequestUserContextResolver.CurrentUser unauthorizedApprover() {
        return new RequestUserContextResolver.CurrentUser(
                99L,
                "approver99",
                "Wrong Approver",
                "Biology",
                List.of("LAB_ROOM_MANAGER"),
                List.of("lab-usage:approve", "lab-usage:list")
        );
    }

    private RequestUserContextResolver.CurrentUser labManagerApprover() {
        return new RequestUserContextResolver.CurrentUser(
                15L,
                "manager15",
                "Current Manager",
                "Biology",
                List.of("LAB_ROOM_MANAGER"),
                List.of("lab-usage:approve", "lab-usage:list")
        );
    }

    private RequestUserContextResolver.CurrentUser deanApprover() {
        return new RequestUserContextResolver.CurrentUser(
                41L,
                "dean41",
                "Dean",
                "College",
                List.of("DEAN"),
                List.of("lab-usage:approve", "lab-usage:list")
        );
    }
}
