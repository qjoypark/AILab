package com.lab.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.approval.client.InventoryClient;
import com.lab.approval.dto.ApprovalContext;
import com.lab.approval.dto.ApprovalFlowDefinition;
import com.lab.approval.dto.ApprovalProcessRequest;
import com.lab.approval.dto.ApprovalRecordDTO;
import com.lab.approval.dto.ApproverCandidateDTO;
import com.lab.approval.dto.CreateLabUsageApplicationRequest;
import com.lab.approval.dto.LabUsageApplicationDTO;
import com.lab.approval.dto.LabUsageParticipantDTO;
import com.lab.approval.dto.LabUsageScheduleDTO;
import com.lab.approval.entity.ApprovalFlowConfig;
import com.lab.approval.entity.ApprovalRecord;
import com.lab.approval.entity.LabRoom;
import com.lab.approval.entity.LabRoomManager;
import com.lab.approval.entity.LabUsageApplication;
import com.lab.approval.entity.LabUsageParticipant;
import com.lab.approval.mapper.ApprovalFlowConfigMapper;
import com.lab.approval.mapper.ApprovalRecordMapper;
import com.lab.approval.mapper.LabRoomManagerMapper;
import com.lab.approval.mapper.LabRoomMapper;
import com.lab.approval.mapper.LabUsageApplicationMapper;
import com.lab.approval.mapper.LabUsageParticipantMapper;
import com.lab.approval.service.ApprovalWorkflowService;
import com.lab.approval.service.ApproverAssignmentService;
import com.lab.approval.service.LabUsageApplicationService;
import com.lab.approval.support.RequestUserContextResolver;
import com.lab.common.exception.BusinessException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabUsageApplicationServiceImpl implements LabUsageApplicationService {

    private static final int BUSINESS_TYPE_LAB_USAGE = 3;
    private static final int ROOM_STATUS_ENABLED = 1;
    private static final int APPLICATION_STATUS_PENDING = 1;
    private static final int APPLICATION_STATUS_APPROVING = 2;
    private static final int APPLICATION_STATUS_APPROVED = 3;
    private static final int APPLICATION_STATUS_REJECTED = 4;
    private static final int APPLICATION_STATUS_CANCELLED = 5;
    private static final int APPROVAL_STATUS_APPROVING = 1;
    private static final int APPROVAL_STATUS_APPROVED = 2;
    private static final int APPROVAL_STATUS_REJECTED = 3;
    private static final int NOTIFICATION_TYPE_APPROVAL = 1;
    private static final String BUSINESS_TYPE_LAB_USAGE_APPLICATION = "LAB_USAGE_APPLICATION";

    private final LabUsageApplicationMapper applicationMapper;
    private final LabUsageParticipantMapper participantMapper;
    private final LabRoomMapper labRoomMapper;
    private final LabRoomManagerMapper labRoomManagerMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalFlowConfigMapper approvalFlowConfigMapper;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApproverAssignmentService approverAssignmentService;
    private final InventoryClient inventoryClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApplication(CreateLabUsageApplicationRequest request,
                                  RequestUserContextResolver.CurrentUser currentUser) {
        requirePermission(currentUser, "lab-usage:create");
        validateCreateRequest(request);
        LabRoom room = requireEnabledLabRoom(request.getLabRoomId());
        ensureLabRoomHasManager(room.getId());

        LabUsageApplication application = new LabUsageApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setApplicantId(currentUser.userId());
        application.setApplicantName(currentUser.displayName());
        application.setApplicantDept(currentUser.department());
        application.setLabRoomId(room.getId());
        application.setLabRoomCode(room.getRoomCode());
        application.setLabRoomName(room.getRoomName());
        application.setUsageType(request.getUsageType() == null ? 1 : request.getUsageType());
        application.setUsagePurpose(request.getUsagePurpose().trim());
        application.setProjectName(trimToNull(request.getProjectName()));
        application.setExpectedAttendeeCount(request.getExpectedAttendeeCount());
        application.setStartTime(request.getStartTime());
        application.setEndTime(request.getEndTime());
        application.setSpecialEquipment(trimToNull(request.getSpecialEquipment()));
        application.setSafetyCommitment(request.getSafetyCommitment() == null ? 0 : request.getSafetyCommitment());
        application.setStatus(APPLICATION_STATUS_PENDING);
        application.setApprovalStatus(APPROVAL_STATUS_APPROVING);
        application.setRemark(trimToNull(request.getRemark()));
        application.setCreatedBy(currentUser.userId());
        application.setCreatedTime(LocalDateTime.now());
        application.setUpdatedBy(currentUser.userId());
        application.setUpdatedTime(LocalDateTime.now());
        application.setDeleted(0);
        applicationMapper.insert(application);

        saveParticipants(application.getId(), request, currentUser);

        ApprovalContext context = buildApprovalContext(application);
        Long firstApproverId = approvalWorkflowService.initializeApprovalWorkflow(
                application.getId(),
                application.getApplicationNo(),
                context
        );

        String firstApproverRole = resolveApproverRoleByLevel(1, context);
        application.setStatus(APPLICATION_STATUS_APPROVING);
        application.setCurrentApproverId(firstApproverId);
        application.setCurrentApproverRole(firstApproverRole);
        application.setUpdatedTime(LocalDateTime.now());
        applicationMapper.updateById(application);
        notifyCurrentLabUsageApprovers(
                application,
                "实验室使用申请待审批",
                "申请单 " + application.getApplicationNo() + " 由 " + application.getApplicantName()
                        + " 提交，使用实验室 " + application.getLabRoomName() + "，等待您审批。"
        );

        log.info("Created lab usage application: id={}, no={}, room={}",
                application.getId(), application.getApplicationNo(), room.getRoomCode());
        return application.getId();
    }

    @Override
    public Page<LabUsageApplicationDTO> listApplications(int page,
                                                         int size,
                                                         Integer status,
                                                         Long labRoomId,
                                                         String keyword,
                                                         LocalDateTime startTime,
                                                         LocalDateTime endTime,
                                                         RequestUserContextResolver.CurrentUser currentUser) {
        requirePermission(currentUser, "lab-usage:list");
        LambdaQueryWrapper<LabUsageApplication> wrapper = new LambdaQueryWrapper<>();
        applyVisibilityFilter(wrapper, currentUser);
        applyCommonFilters(wrapper, status, labRoomId, keyword, startTime, endTime);
        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        long total = applicationMapper.selectCount(wrapper);
        wrapper.orderByDesc(LabUsageApplication::getCreatedTime)
                .last("LIMIT " + ((long) (currentPage - 1) * pageSize) + ", " + pageSize);

        Page<LabUsageApplicationDTO> dtoPage = new Page<>(currentPage, pageSize, total);
        dtoPage.setRecords(applicationMapper.selectList(wrapper).stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    public List<LabUsageApplicationDTO> listPendingApplications(RequestUserContextResolver.CurrentUser currentUser) {
        requirePermission(currentUser, "lab-usage:approve");
        LambdaQueryWrapper<LabUsageApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabUsageApplication::getStatus, APPLICATION_STATUS_APPROVING)
                .eq(LabUsageApplication::getApprovalStatus, APPROVAL_STATUS_APPROVING)
                .orderByAsc(LabUsageApplication::getCreatedTime);
        return applicationMapper.selectList(wrapper).stream()
                .filter(application -> canUserApproveApplication(application, currentUser))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public LabUsageApplicationDTO getApplicationDetail(Long id,
                                                       RequestUserContextResolver.CurrentUser currentUser) {
        requirePermission(currentUser, "lab-usage:list");
        LabUsageApplication application = requireApplication(id);
        if (!canViewApplication(application, currentUser)) {
            throw new BusinessException(403001, "No permission to view this lab usage application.");
        }
        return toDtoWithApprovalRecords(application);
    }

    @Override
    public byte[] generateApplicationPdf(Long id,
                                         RequestUserContextResolver.CurrentUser currentUser) {
        LabUsageApplicationDTO application = getApplicationDetail(id, currentUser);
        if (!Objects.equals(application.getStatus(), APPLICATION_STATUS_APPROVED)
                && !Objects.equals(application.getApprovalStatus(), APPROVAL_STATUS_APPROVED)) {
            throw new BusinessException("审批通过后才能生成实验室使用申请单");
        }

        List<LabUsageParticipantDTO> participants = resolvePdfParticipants(application);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 42, 36);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font subTitleFont = new Font(baseFont, 11, Font.NORMAL);
            Font sectionFont = new Font(baseFont, 12, Font.BOLD);
            Font headerFont = new Font(baseFont, 10, Font.BOLD);
            Font contentFont = new Font(baseFont, 10, Font.NORMAL);

            Paragraph title = new Paragraph("呼伦贝尔学院农学院实验室使用申请单", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);

            Paragraph docNo = new Paragraph("电子单号：" + fallbackValue(application.getApplicationNo()), subTitleFont);
            docNo.setAlignment(Element.ALIGN_RIGHT);
            docNo.setSpacingAfter(8f);
            document.add(docNo);

            PdfPTable metaTable = new PdfPTable(new float[]{1.25f, 2.1f, 1.25f, 2.1f});
            metaTable.setWidthPercentage(100);
            addMetaRow(metaTable, headerFont, contentFont,
                    "申请单号", fallbackValue(application.getApplicationNo()),
                    "申请时间", formatDateTime(application.getCreatedTime()));
            addMetaRow(metaTable, headerFont, contentFont,
                    "申请教师", fallbackValue(application.getApplicantName()),
                    "所属部门", fallbackValue(application.getApplicantDept()));
            addMetaRow(metaTable, headerFont, contentFont,
                    "实验室", formatLabRoomName(application),
                    "使用类型", usageTypeName(application.getUsageType()));
            addMetaRow(metaTable, headerFont, contentFont,
                    "使用时段", formatDateTime(application.getStartTime()) + " 至 " + formatDateTime(application.getEndTime()),
                    "预计人数", application.getExpectedAttendeeCount() == null ? "-" : String.valueOf(application.getExpectedAttendeeCount()));
            addMetaRow(metaTable, headerFont, contentFont,
                    "课程/项目", fallbackValue(application.getProjectName()),
                    "安全承诺", Objects.equals(application.getSafetyCommitment(), 1) ? "已承诺" : "未承诺");
            metaTable.setSpacingAfter(10f);
            document.add(metaTable);

            addTextSection(document, sectionFont, contentFont, "使用用途", application.getUsagePurpose());
            addTextSection(document, sectionFont, contentFont, "特殊设备或支持需求", application.getSpecialEquipment());
            addTextSection(document, sectionFont, contentFont, "备注", application.getRemark());

            Paragraph participantTitle = new Paragraph("共同使用教师", sectionFont);
            participantTitle.setSpacingBefore(8f);
            participantTitle.setSpacingAfter(6f);
            document.add(participantTitle);

            PdfPTable participantTable = new PdfPTable(new float[]{0.7f, 1.5f, 2.2f, 1.1f});
            participantTable.setWidthPercentage(100);
            addHeaderCell(participantTable, headerFont, "序号");
            addHeaderCell(participantTable, headerFont, "姓名");
            addHeaderCell(participantTable, headerFont, "部门");
            addHeaderCell(participantTable, headerFont, "用户ID");
            for (int index = 0; index < participants.size(); index++) {
                LabUsageParticipantDTO participant = participants.get(index);
                addContentCell(participantTable, contentFont, String.valueOf(index + 1), Element.ALIGN_CENTER);
                addContentCell(participantTable, contentFont, participant.getRealName(), Element.ALIGN_LEFT);
                addContentCell(participantTable, contentFont, participant.getDeptName(), Element.ALIGN_LEFT);
                addContentCell(participantTable, contentFont,
                        participant.getUserId() == null ? "-" : String.valueOf(participant.getUserId()),
                        Element.ALIGN_CENTER);
            }
            participantTable.setSpacingAfter(10f);
            document.add(participantTable);

            Paragraph approvalTitle = new Paragraph("审批记录", sectionFont);
            approvalTitle.setSpacingBefore(6f);
            approvalTitle.setSpacingAfter(6f);
            document.add(approvalTitle);

            PdfPTable approvalTable = new PdfPTable(new float[]{0.8f, 1.4f, 1.0f, 2.6f, 1.8f});
            approvalTable.setWidthPercentage(100);
            addHeaderCell(approvalTable, headerFont, "级次");
            addHeaderCell(approvalTable, headerFont, "审批人");
            addHeaderCell(approvalTable, headerFont, "结果");
            addHeaderCell(approvalTable, headerFont, "意见");
            addHeaderCell(approvalTable, headerFont, "时间");
            List<ApprovalRecordDTO> approvalRecords = application.getApprovalRecords() == null
                    ? Collections.emptyList()
                    : application.getApprovalRecords();
            if (approvalRecords.isEmpty()) {
                addContentCell(approvalTable, contentFont, "-", Element.ALIGN_CENTER);
                addContentCell(approvalTable, contentFont, "-", Element.ALIGN_CENTER);
                addContentCell(approvalTable, contentFont, "-", Element.ALIGN_CENTER);
                addContentCell(approvalTable, contentFont, "暂无审批记录", Element.ALIGN_LEFT);
                addContentCell(approvalTable, contentFont, "-", Element.ALIGN_CENTER);
            } else {
                for (ApprovalRecordDTO record : approvalRecords) {
                    addContentCell(approvalTable, contentFont,
                            record.getApprovalLevel() == null ? "-" : String.valueOf(record.getApprovalLevel()),
                            Element.ALIGN_CENTER);
                    addContentCell(approvalTable, contentFont, record.getApproverName(), Element.ALIGN_LEFT);
                    addContentCell(approvalTable, contentFont, approvalResultName(record.getApprovalResult()), Element.ALIGN_CENTER);
                    addContentCell(approvalTable, contentFont, record.getApprovalOpinion(), Element.ALIGN_LEFT);
                    addContentCell(approvalTable, contentFont, formatDateTime(record.getApprovalTime()), Element.ALIGN_CENTER);
                }
            }
            approvalTable.setSpacingAfter(12f);
            document.add(approvalTable);

            PdfPTable signatureTable = new PdfPTable(new float[]{1f, 1f, 1.3f});
            signatureTable.setWidthPercentage(100);
            addSignatureCell(signatureTable, subTitleFont, "申请人签字：");
            addSignatureCell(signatureTable, subTitleFont, "实验室管理人员确认：");
            addSignatureCell(signatureTable, subTitleFont, "生成时间：" + formatDateTime(LocalDateTime.now()));
            signatureTable.setSpacingBefore(6f);
            document.add(signatureTable);

            Paragraph note = new Paragraph("备注：本电子申请单由系统根据已通过的审批记录生成，可用于打印、归档和现场核验。", subTitleFont);
            note.setSpacingBefore(8f);
            document.add(note);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to generate lab usage application PDF: applicationId={}", id, ex);
            throw new BusinessException("生成实验室使用申请单PDF失败");
        }
    }

    @Override
    public List<LabUsageApplicationDTO> listOverlapApplications(Long labRoomId,
                                                               LocalDateTime startTime,
                                                               LocalDateTime endTime,
                                                               RequestUserContextResolver.CurrentUser currentUser) {
        requireAnyPermission(currentUser, "lab-usage:create", "lab-usage:list");
        if (labRoomId == null) {
            throw new BusinessException("Lab room is required.");
        }
        if (startTime == null || endTime == null) {
            throw new BusinessException("Usage start time and end time are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("Usage end time must be after start time.");
        }

        LambdaQueryWrapper<LabUsageApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabUsageApplication::getLabRoomId, labRoomId)
                .in(LabUsageApplication::getStatus, APPLICATION_STATUS_APPROVING, APPLICATION_STATUS_APPROVED)
                .lt(LabUsageApplication::getStartTime, endTime)
                .gt(LabUsageApplication::getEndTime, startTime)
                .orderByAsc(LabUsageApplication::getStartTime);

        return applicationMapper.selectList(wrapper).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApplication(Long id, RequestUserContextResolver.CurrentUser currentUser) {
        LabUsageApplication application = requireApplication(id);
        if (!Objects.equals(application.getApplicantId(), currentUser.userId())) {
            requirePermission(currentUser, "lab-usage:cancel");
        }
        if (!Objects.equals(application.getApplicantId(), currentUser.userId()) && !isAdmin(currentUser)) {
            throw new BusinessException(403001, "Only applicant or admin can cancel this application.");
        }
        if (!Objects.equals(application.getStatus(), APPLICATION_STATUS_PENDING)
                && !Objects.equals(application.getStatus(), APPLICATION_STATUS_APPROVING)) {
            throw new BusinessException("Only pending or approving applications can be cancelled.");
        }

        application.setStatus(APPLICATION_STATUS_CANCELLED);
        application.setCurrentApproverId(null);
        application.setCurrentApproverRole(null);
        application.setUpdatedBy(currentUser.userId());
        application.setUpdatedTime(LocalDateTime.now());
        applicationMapper.updateById(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processApproval(Long id,
                                ApprovalProcessRequest request,
                                RequestUserContextResolver.CurrentUser currentUser) {
        requirePermission(currentUser, "lab-usage:approve");
        if (request == null || request.getApprovalResult() == null) {
            throw new BusinessException("Approval result is required.");
        }
        if (request.getApprovalResult() != 1 && request.getApprovalResult() != 2) {
            throw new BusinessException("Unsupported approval result.");
        }

        LabUsageApplication application = requireApplication(id);
        if (!Objects.equals(application.getStatus(), APPLICATION_STATUS_APPROVING)) {
            throw new BusinessException("Application is not under approval.");
        }
        if (!canUserApproveApplication(application, currentUser)) {
            throw new BusinessException(403001, "No permission to approve this lab usage application.");
        }

        List<ApprovalRecord> existingRecords = approvalWorkflowService.getApprovalHistory(BUSINESS_TYPE_LAB_USAGE, id);
        int currentLevel = existingRecords.size() + 1;

        ApprovalRecord record = new ApprovalRecord();
        record.setApplicationId(application.getId());
        record.setApplicationNo(application.getApplicationNo());
        record.setBusinessType(BUSINESS_TYPE_LAB_USAGE);
        record.setBusinessNo(application.getApplicationNo());
        record.setApproverId(currentUser.userId());
        record.setApproverName(currentUser.displayName());
        record.setApprovalLevel(currentLevel);
        record.setApprovalResult(request.getApprovalResult());
        record.setApprovalOpinion(request.getApprovalOpinion());
        record.setApprovalTime(LocalDateTime.now());
        record.setCreatedTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);

        if (request.getApprovalResult() == 1) {
            handleApprovalPass(application, currentLevel, currentUser);
        } else {
            handleApprovalReject(application, currentUser);
        }
    }

    @Override
    public List<LabUsageScheduleDTO> listApprovedSchedules(Long labRoomId,
                                                           Long teacherId,
                                                           LocalDateTime startTime,
                                                           LocalDateTime endTime,
                                                           RequestUserContextResolver.CurrentUser currentUser) {
        requirePermission(currentUser, "lab-usage:schedule:view");
        LambdaQueryWrapper<LabUsageApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabUsageApplication::getStatus, APPLICATION_STATUS_APPROVED)
                .eq(LabUsageApplication::getApprovalStatus, APPROVAL_STATUS_APPROVED);
        if (labRoomId != null) {
            wrapper.eq(LabUsageApplication::getLabRoomId, labRoomId);
        }
        applyTimeOverlapFilter(wrapper, startTime, endTime);
        if (teacherId != null) {
            List<Long> applicationIds = selectParticipantApplicationIds(teacherId);
            if (applicationIds.isEmpty()) {
                wrapper.eq(LabUsageApplication::getApplicantId, teacherId);
            } else {
                wrapper.and(condition -> condition
                        .eq(LabUsageApplication::getApplicantId, teacherId)
                        .or()
                        .in(LabUsageApplication::getId, applicationIds));
            }
        }
        wrapper.orderByAsc(LabUsageApplication::getStartTime);
        return applicationMapper.selectList(wrapper).stream()
                .map(this::toScheduleDto)
                .collect(Collectors.toList());
    }

    private void validateCreateRequest(CreateLabUsageApplicationRequest request) {
        if (request == null) {
            throw new BusinessException("Application request is required.");
        }
        if (request.getLabRoomId() == null) {
            throw new BusinessException("Lab room is required.");
        }
        if (!StringUtils.hasText(request.getUsagePurpose())) {
            throw new BusinessException("Usage purpose is required.");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException("Usage start time and end time are required.");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("Usage end time must be after start time.");
        }
        if (request.getExpectedAttendeeCount() != null && request.getExpectedAttendeeCount() < 0) {
            throw new BusinessException("Expected attendee count cannot be negative.");
        }
    }

    private LabRoom requireEnabledLabRoom(Long labRoomId) {
        LabRoom room = labRoomMapper.selectById(labRoomId);
        if (room == null) {
            throw new BusinessException("Lab room does not exist.");
        }
        if (!Objects.equals(room.getStatus(), ROOM_STATUS_ENABLED)) {
            throw new BusinessException("Lab room is not enabled.");
        }
        return room;
    }

    private LabUsageApplication requireApplication(Long id) {
        if (id == null) {
            throw new BusinessException("Application id is required.");
        }
        LabUsageApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("Lab usage application does not exist.");
        }
        return application;
    }

    private void ensureLabRoomHasManager(Long labRoomId) {
        LambdaQueryWrapper<LabRoomManager> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabRoomManager::getLabRoomId, labRoomId)
                .eq(LabRoomManager::getStatus, ROOM_STATUS_ENABLED);
        if (labRoomManagerMapper.selectCount(wrapper) <= 0) {
            throw new BusinessException("Please configure lab room managers before submitting applications.");
        }
    }

    private void saveParticipants(Long applicationId,
                                  CreateLabUsageApplicationRequest request,
                                  RequestUserContextResolver.CurrentUser currentUser) {
        Map<Long, CreateLabUsageApplicationRequest.ParticipantRequest> participants = new LinkedHashMap<>();

        CreateLabUsageApplicationRequest.ParticipantRequest applicant = new CreateLabUsageApplicationRequest.ParticipantRequest();
        applicant.setUserId(currentUser.userId());
        applicant.setRealName(currentUser.displayName());
        applicant.setDeptName(currentUser.department());
        participants.put(applicant.getUserId(), applicant);

        if (request.getParticipants() != null) {
            for (CreateLabUsageApplicationRequest.ParticipantRequest participant : request.getParticipants()) {
                if (participant == null || participant.getUserId() == null) {
                    continue;
                }
                participants.putIfAbsent(participant.getUserId(), participant);
            }
        }

        List<Long> requestedUserIds = participants.keySet().stream()
                .filter(userId -> !Objects.equals(userId, currentUser.userId()))
                .collect(Collectors.toList());
        Map<Long, LabUsageParticipantDTO> activeUserMap = requestedUserIds.isEmpty()
                ? Collections.emptyMap()
                : participantMapper.selectActiveUsersByIds(requestedUserIds).stream()
                        .collect(Collectors.toMap(LabUsageParticipantDTO::getUserId, Function.identity()));

        for (CreateLabUsageApplicationRequest.ParticipantRequest item : participants.values()) {
            LabUsageParticipantDTO activeUser = null;
            if (!Objects.equals(item.getUserId(), currentUser.userId())) {
                activeUser = activeUserMap.get(item.getUserId());
                if (activeUser == null) {
                    throw new BusinessException("Selected participant does not exist or is disabled: " + item.getUserId());
                }
            }
            LabUsageParticipant participant = new LabUsageParticipant();
            participant.setApplicationId(applicationId);
            participant.setUserId(item.getUserId());
            participant.setRealName(activeUser == null ? currentUser.displayName() : activeUser.getRealName());
            participant.setDeptName(activeUser == null ? currentUser.department() : trimToNull(activeUser.getDeptName()));
            participant.setCreatedTime(LocalDateTime.now());
            participantMapper.insert(participant);
        }
    }

    private void applyVisibilityFilter(LambdaQueryWrapper<LabUsageApplication> wrapper,
                                       RequestUserContextResolver.CurrentUser currentUser) {
        if (canViewAllApplications(currentUser)) {
            return;
        }

        List<Long> managedRoomIds = selectManagedLabRoomIds(currentUser.userId());
        List<Long> participantApplicationIds = selectParticipantApplicationIds(currentUser.userId());
        wrapper.and(condition -> {
            condition.eq(LabUsageApplication::getApplicantId, currentUser.userId());
            if (!managedRoomIds.isEmpty()) {
                condition.or().in(LabUsageApplication::getLabRoomId, managedRoomIds);
            }
            if (!participantApplicationIds.isEmpty()) {
                condition.or().in(LabUsageApplication::getId, participantApplicationIds);
            }
        });
    }

    private void applyCommonFilters(LambdaQueryWrapper<LabUsageApplication> wrapper,
                                    Integer status,
                                    Long labRoomId,
                                    String keyword,
                                    LocalDateTime startTime,
                                    LocalDateTime endTime) {
        if (status != null) {
            wrapper.eq(LabUsageApplication::getStatus, status);
        }
        if (labRoomId != null) {
            wrapper.eq(LabUsageApplication::getLabRoomId, labRoomId);
        }
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(condition -> condition
                    .like(LabUsageApplication::getApplicationNo, trimmed)
                    .or()
                    .like(LabUsageApplication::getApplicantName, trimmed)
                    .or()
                    .like(LabUsageApplication::getLabRoomName, trimmed)
                    .or()
                    .like(LabUsageApplication::getUsagePurpose, trimmed)
                    .or()
                    .like(LabUsageApplication::getProjectName, trimmed));
        }
        applyTimeOverlapFilter(wrapper, startTime, endTime);
    }

    private void applyTimeOverlapFilter(LambdaQueryWrapper<LabUsageApplication> wrapper,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            wrapper.lt(LabUsageApplication::getStartTime, endTime)
                    .gt(LabUsageApplication::getEndTime, startTime);
        } else if (startTime != null) {
            wrapper.ge(LabUsageApplication::getEndTime, startTime);
        } else if (endTime != null) {
            wrapper.le(LabUsageApplication::getStartTime, endTime);
        }
    }

    private List<Long> selectManagedLabRoomIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<LabRoomManager> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabRoomManager::getManagerId, userId)
                .eq(LabRoomManager::getStatus, ROOM_STATUS_ENABLED);
        return labRoomManagerMapper.selectList(wrapper).stream()
                .map(LabRoomManager::getLabRoomId)
                .collect(Collectors.toList());
    }

    private List<Long> selectParticipantApplicationIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<LabUsageParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabUsageParticipant::getUserId, userId);
        return participantMapper.selectList(wrapper).stream()
                .map(LabUsageParticipant::getApplicationId)
                .collect(Collectors.toList());
    }

    private List<LabUsageParticipantDTO> listParticipantDtos(Long applicationId) {
        LambdaQueryWrapper<LabUsageParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabUsageParticipant::getApplicationId, applicationId)
                .orderByAsc(LabUsageParticipant::getId);
        return participantMapper.selectList(wrapper).stream()
                .map(LabUsageParticipantDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private LabUsageApplicationDTO toDto(LabUsageApplication application) {
        fillCurrentApprovalInfo(application);
        LabUsageApplicationDTO dto = LabUsageApplicationDTO.fromEntity(application);
        dto.setParticipants(listParticipantDtos(application.getId()));
        return dto;
    }

    private LabUsageApplicationDTO toDtoWithApprovalRecords(LabUsageApplication application) {
        LabUsageApplicationDTO dto = toDto(application);
        dto.setApprovalRecords(approvalWorkflowService
                .getApprovalHistory(BUSINESS_TYPE_LAB_USAGE, application.getId())
                .stream()
                .map(this::toApprovalRecordDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private ApprovalRecordDTO toApprovalRecordDto(ApprovalRecord record) {
        ApprovalRecordDTO dto = new ApprovalRecordDTO();
        dto.setId(record.getId());
        dto.setApproverName(record.getApproverName());
        dto.setApprovalLevel(record.getApprovalLevel());
        dto.setApprovalResult(record.getApprovalResult());
        dto.setApprovalOpinion(record.getApprovalOpinion());
        dto.setApprovalTime(record.getApprovalTime());
        return dto;
    }

    private LabUsageScheduleDTO toScheduleDto(LabUsageApplication application) {
        LabUsageScheduleDTO dto = new LabUsageScheduleDTO();
        dto.setApplicationId(application.getId());
        dto.setApplicationNo(application.getApplicationNo());
        dto.setLabRoomId(application.getLabRoomId());
        dto.setLabRoomCode(application.getLabRoomCode());
        dto.setLabRoomName(application.getLabRoomName());
        dto.setApplicantName(application.getApplicantName());
        dto.setApplicantDept(application.getApplicantDept());
        dto.setUsagePurpose(application.getUsagePurpose());
        dto.setProjectName(application.getProjectName());
        dto.setStartTime(application.getStartTime());
        dto.setEndTime(application.getEndTime());
        dto.setParticipants(listParticipantDtos(application.getId()));
        return dto;
    }

    private void handleApprovalPass(LabUsageApplication application,
                                    int currentLevel,
                                    RequestUserContextResolver.CurrentUser currentUser) {
        ApprovalContext context = buildApprovalContext(application);
        int levelCount = resolveLevelCount();
        if (currentLevel < levelCount) {
            String nextRole = resolveApproverRoleByLevel(currentLevel + 1, context);
            Long nextApproverId = assignApproverByLevel(currentLevel + 1, context);
            if (nextApproverId == null) {
                throw new BusinessException("Cannot assign next approval level approver.");
            }
            application.setCurrentApproverId(nextApproverId);
            application.setCurrentApproverRole(nextRole);
            application.setStatus(APPLICATION_STATUS_APPROVING);
            application.setApprovalStatus(APPROVAL_STATUS_APPROVING);
            notifyCurrentLabUsageApprovers(
                    application,
                    "实验室使用申请待审批",
                    "申请单 " + application.getApplicationNo() + " 已进入下一审批节点，等待您审批。"
            );
        } else {
            application.setStatus(APPLICATION_STATUS_APPROVED);
            application.setApprovalStatus(APPROVAL_STATUS_APPROVED);
            application.setCurrentApproverId(null);
            application.setCurrentApproverRole(null);
            notifyLabUsageApplicant(
                    application,
                    "实验室使用申请已通过",
                    "您的申请单 " + application.getApplicationNo() + " 已审批通过，系统日程将显示该时段使用安排。"
            );
        }
        application.setUpdatedBy(currentUser.userId());
        application.setUpdatedTime(LocalDateTime.now());
        applicationMapper.updateById(application);
    }

    private void handleApprovalReject(LabUsageApplication application,
                                      RequestUserContextResolver.CurrentUser currentUser) {
        application.setStatus(APPLICATION_STATUS_REJECTED);
        application.setApprovalStatus(APPROVAL_STATUS_REJECTED);
        application.setCurrentApproverId(null);
        application.setCurrentApproverRole(null);
        application.setUpdatedBy(currentUser.userId());
        application.setUpdatedTime(LocalDateTime.now());
        applicationMapper.updateById(application);
        notifyLabUsageApplicant(
                application,
                "实验室使用申请被驳回",
                "您的申请单 " + application.getApplicationNo() + " 未通过审批，请查看审批意见。"
        );
    }

    private void notifyCurrentLabUsageApprovers(LabUsageApplication application, String title, String content) {
        if (application == null || application.getId() == null) {
            return;
        }
        Set<Long> receiverIds = new LinkedHashSet<>();
        if (application.getCurrentApproverId() != null) {
            receiverIds.add(application.getCurrentApproverId());
        }
        receiverIds.addAll(resolveCurrentApproverCandidates(application).stream()
                .map(ApproverCandidateDTO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        sendLabUsageNotificationsAfterCommit(receiverIds, title, content, application.getId());
    }

    private void notifyLabUsageApplicant(LabUsageApplication application, String title, String content) {
        if (application == null || application.getApplicantId() == null) {
            return;
        }
        sendLabUsageNotificationAfterCommit(application.getApplicantId(), title, content, application.getId());
    }

    private void sendLabUsageNotificationsAfterCommit(Set<Long> receiverIds,
                                                      String title,
                                                      String content,
                                                      Long businessId) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }
        for (Long receiverId : receiverIds) {
            sendLabUsageNotificationAfterCommit(receiverId, title, content, businessId);
        }
    }

    private void sendLabUsageNotificationAfterCommit(Long receiverId,
                                                     String title,
                                                     String content,
                                                     Long businessId) {
        if (receiverId == null) {
            return;
        }
        Runnable task = () -> inventoryClient.sendNotification(
                receiverId,
                NOTIFICATION_TYPE_APPROVAL,
                title,
                content,
                BUSINESS_TYPE_LAB_USAGE_APPLICATION,
                businessId
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }

    private boolean canUserApproveApplication(LabUsageApplication application,
                                              RequestUserContextResolver.CurrentUser currentUser) {
        if (application == null || currentUser == null || currentUser.userId() == null) {
            return false;
        }
        if (isAdmin(currentUser)) {
            return true;
        }
        if (!Objects.equals(application.getStatus(), APPLICATION_STATUS_APPROVING)) {
            return false;
        }
        List<Long> approverIds = resolveCurrentApproverCandidates(application).stream()
                .map(ApproverCandidateDTO::getUserId)
                .collect(Collectors.toList());
        return approverIds.contains(currentUser.userId())
                || Objects.equals(application.getCurrentApproverId(), currentUser.userId());
    }

    private boolean canViewApplication(LabUsageApplication application,
                                       RequestUserContextResolver.CurrentUser currentUser) {
        if (canViewAllApplications(currentUser)) {
            return true;
        }
        if (Objects.equals(application.getApplicantId(), currentUser.userId())) {
            return true;
        }
        if (selectManagedLabRoomIds(currentUser.userId()).contains(application.getLabRoomId())) {
            return true;
        }
        return selectParticipantApplicationIds(currentUser.userId()).contains(application.getId());
    }

    private boolean canViewAllApplications(RequestUserContextResolver.CurrentUser currentUser) {
        return currentUser.hasAnyRole(
                "ADMIN",
                "CENTER_ADMIN",
                "CENTER_DIRECTOR",
                "003",
                "DEPUTY_DEAN",
                "002",
                "DEAN",
                "001"
        );
    }

    private boolean isAdmin(RequestUserContextResolver.CurrentUser currentUser) {
        return currentUser.hasRole("ADMIN");
    }

    private void requirePermission(RequestUserContextResolver.CurrentUser currentUser, String permissionCode) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (currentUser.permissions() != null && currentUser.permissions().contains(permissionCode)) {
            return;
        }
        throw new BusinessException(403001, "No permission: " + permissionCode);
    }

    private void requireAnyPermission(RequestUserContextResolver.CurrentUser currentUser, String... permissionCodes) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (currentUser.permissions() != null && permissionCodes != null) {
            for (String permissionCode : permissionCodes) {
                if (currentUser.permissions().contains(permissionCode)) {
                    return;
                }
            }
        }
        throw new BusinessException(403001, "No permission.");
    }

    private void fillCurrentApprovalInfo(LabUsageApplication application) {
        if (application == null || application.getId() == null) {
            return;
        }
        if (!Objects.equals(application.getStatus(), APPLICATION_STATUS_PENDING)
                && !Objects.equals(application.getStatus(), APPLICATION_STATUS_APPROVING)) {
            application.setCurrentApproverId(null);
            application.setCurrentApproverRole(null);
            application.setCurrentApproverName(null);
            application.setCurrentApproverIds(Collections.emptyList());
            application.setCurrentApproverNames(Collections.emptyList());
            application.setCurrentPendingStatus(resolveReadablePendingStatus(application));
            return;
        }
        List<ApproverCandidateDTO> candidates = resolveCurrentApproverCandidates(application);
        application.setCurrentApproverRole(resolveCurrentApproverRole(application));
        application.setCurrentApproverIds(candidates.stream()
                .map(ApproverCandidateDTO::getUserId)
                .collect(Collectors.toList()));
        application.setCurrentApproverNames(candidates.stream()
                .map(ApproverCandidateDTO::getDisplayName)
                .collect(Collectors.toList()));
        application.setCurrentApproverName(candidates.isEmpty() ? "" : candidates.get(0).getDisplayName());
        application.setCurrentPendingStatus(resolveReadablePendingStatus(application));
    }

    private String resolveReadablePendingStatus(LabUsageApplication application) {
        if (application == null || application.getStatus() == null) {
            return "待处理";
        }
        if (application.getStatus() == APPLICATION_STATUS_APPROVED) {
            return "审批通过";
        }
        if (application.getStatus() == APPLICATION_STATUS_REJECTED) {
            return "审批拒绝";
        }
        if (application.getStatus() == APPLICATION_STATUS_CANCELLED) {
            return "已取消";
        }
        if (application.getCurrentApproverIds() == null || application.getCurrentApproverIds().isEmpty()) {
            return "待分配审批人";
        }
        return "待审批";
    }

    private String resolvePendingStatus(LabUsageApplication application) {
        if (application == null || application.getStatus() == null) {
            return "待处理";
        }
        if (application.getStatus() == APPLICATION_STATUS_APPROVED) {
            return "审批通过";
        }
        if (application.getStatus() == APPLICATION_STATUS_REJECTED) {
            return "审批拒绝";
        }
        if (application.getStatus() == APPLICATION_STATUS_CANCELLED) {
            return "已取消";
        }
        if (application.getCurrentApproverIds() == null || application.getCurrentApproverIds().isEmpty()) {
            return "待分配审批人";
        }
        return "待审批";
    }

    private List<ApproverCandidateDTO> resolveCurrentApproverCandidates(LabUsageApplication application) {
        String approverRole = resolveCurrentApproverRole(application);
        if (!StringUtils.hasText(approverRole)) {
            return Collections.emptyList();
        }
        List<ApproverCandidateDTO> candidates = approverAssignmentService.listApproverCandidates(
                approverRole,
                buildApprovalContext(application)
        );
        List<ApproverCandidateDTO> safeCandidates = new ArrayList<>(
                candidates == null ? Collections.emptyList() : candidates
        );
        if (application.getCurrentApproverId() != null) {
            boolean exists = safeCandidates.stream()
                    .anyMatch(candidate -> Objects.equals(candidate.getUserId(), application.getCurrentApproverId()));
            if (!exists) {
                ApproverCandidateDTO fallback = new ApproverCandidateDTO();
                fallback.setUserId(application.getCurrentApproverId());
                fallback.setRoleCode(approverRole);
                fallback.setRealName("USER#" + application.getCurrentApproverId());
                safeCandidates.add(0, fallback);
            }
        }
        return safeCandidates;
    }

    private String resolveCurrentApproverRole(LabUsageApplication application) {
        if (application == null || application.getId() == null) {
            return null;
        }
        List<ApprovalRecord> records = approvalWorkflowService.getApprovalHistory(
                BUSINESS_TYPE_LAB_USAGE,
                application.getId()
        );
        return resolveApproverRoleByLevel(records.size() + 1, buildApprovalContext(application));
    }

    private String resolveApproverRoleByLevel(int level, ApprovalContext context) {
        ApprovalFlowDefinition flowDefinition = resolveFlowDefinition();
        if (flowDefinition == null || flowDefinition.getLevels() == null || flowDefinition.getLevels().size() < level) {
            return null;
        }
        ApprovalFlowDefinition.ApprovalLevel levelConfig = flowDefinition.getLevels().get(level - 1);
        return approverAssignmentService.resolveEffectiveRoleCode(levelConfig.getApproverRole(), context);
    }

    private Long assignApproverByLevel(int level, ApprovalContext context) {
        ApprovalFlowDefinition flowDefinition = resolveFlowDefinition();
        if (flowDefinition == null || flowDefinition.getLevels() == null || flowDefinition.getLevels().size() < level) {
            return null;
        }
        return approverAssignmentService.assignApprover(flowDefinition.getLevels().get(level - 1).getApproverRole(), context);
    }

    private int resolveLevelCount() {
        ApprovalFlowDefinition flowDefinition = resolveFlowDefinition();
        return flowDefinition == null || flowDefinition.getLevels() == null ? 0 : flowDefinition.getLevels().size();
    }

    private ApprovalFlowDefinition resolveFlowDefinition() {
        try {
            LambdaQueryWrapper<ApprovalFlowConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApprovalFlowConfig::getBusinessType, BUSINESS_TYPE_LAB_USAGE)
                    .eq(ApprovalFlowConfig::getStatus, 1)
                    .last("LIMIT 1");
            ApprovalFlowConfig config = approvalFlowConfigMapper.selectOne(wrapper);
            if (config == null || !StringUtils.hasText(config.getFlowDefinition())) {
                return defaultLabUsageFlowDefinition();
            }
            return objectMapper.readValue(config.getFlowDefinition(), ApprovalFlowDefinition.class);
        } catch (Exception exception) {
            log.warn("Failed to parse lab usage approval flow, using default flow.", exception);
            return defaultLabUsageFlowDefinition();
        }
    }

    private ApprovalFlowDefinition defaultLabUsageFlowDefinition() {
        ApprovalFlowDefinition definition = new ApprovalFlowDefinition();
        List<ApprovalFlowDefinition.ApprovalLevel> levels = new ArrayList<>();
        levels.add(buildLevel(1, "LAB_ROOM_MANAGER", "Lab room manager"));
        levels.add(buildLevel(2, "CENTER_DIRECTOR", "Center director"));
        levels.add(buildLevel(3, "DEPUTY_DEAN", "Deputy dean"));
        levels.add(buildLevel(4, "DEAN", "Dean"));
        definition.setLevels(levels);
        return definition;
    }

    private ApprovalFlowDefinition.ApprovalLevel buildLevel(int level, String role, String name) {
        ApprovalFlowDefinition.ApprovalLevel approvalLevel = new ApprovalFlowDefinition.ApprovalLevel();
        approvalLevel.setLevel(level);
        approvalLevel.setApproverRole(role);
        approvalLevel.setApproverName(name);
        return approvalLevel;
    }

    private ApprovalContext buildApprovalContext(LabUsageApplication application) {
        ApprovalContext context = new ApprovalContext();
        context.setBusinessType(BUSINESS_TYPE_LAB_USAGE);
        context.setApplicationType(BUSINESS_TYPE_LAB_USAGE);
        context.setApplicationId(application.getId());
        context.setApplicationNo(application.getApplicationNo());
        context.setBusinessId(application.getId());
        context.setBusinessNo(application.getApplicationNo());
        context.setApplicantId(application.getApplicantId());
        context.setApplicantDept(application.getApplicantDept());
        context.setLabRoomId(application.getLabRoomId());
        return context;
    }

    private String generateApplicationNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
        return "LAB" + dateStr + randomStr;
    }

    private List<LabUsageParticipantDTO> resolvePdfParticipants(LabUsageApplicationDTO application) {
        List<LabUsageParticipantDTO> participants = new ArrayList<>(
                application.getParticipants() == null ? Collections.emptyList() : application.getParticipants()
        );
        boolean applicantIncluded = participants.stream()
                .anyMatch(participant -> Objects.equals(participant.getUserId(), application.getApplicantId()));
        if (!applicantIncluded) {
            LabUsageParticipantDTO applicant = new LabUsageParticipantDTO();
            applicant.setUserId(application.getApplicantId());
            applicant.setRealName(application.getApplicantName());
            applicant.setDeptName(application.getApplicantDept());
            participants.add(0, applicant);
        }
        return participants;
    }

    private void addTextSection(Document document, Font titleFont, Font contentFont, String title, String content)
            throws Exception {
        Paragraph sectionTitle = new Paragraph(title, titleFont);
        sectionTitle.setSpacingBefore(6f);
        sectionTitle.setSpacingAfter(4f);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(fallbackValue(content), contentFont));
        cell.setPadding(8f);
        cell.setMinimumHeight(32f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        table.addCell(cell);
        table.setSpacingAfter(6f);
        document.add(table);
    }

    private void addMetaRow(PdfPTable table, Font labelFont, Font valueFont,
                            String label1, String value1, String label2, String value2) {
        addLabelCell(table, labelFont, label1);
        addValueCell(table, valueFont, value1);
        addLabelCell(table, labelFont, label2);
        addValueCell(table, valueFont, value2);
    }

    private void addLabelCell(PdfPTable table, Font font, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBackgroundColor(new Color(245, 247, 250));
        table.addCell(cell);
    }

    private void addValueCell(PdfPTable table, Font font, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(fallbackValue(text), font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, Font font, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBackgroundColor(new Color(235, 245, 255));
        table.addCell(cell);
    }

    private void addContentCell(PdfPTable table, Font font, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(fallbackValue(text), font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addSignatureCell(PdfPTable table, Font font, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        cell.setMinimumHeight(38f);
        table.addCell(cell);
    }

    private String formatLabRoomName(LabUsageApplicationDTO application) {
        String roomName = fallbackValue(application.getLabRoomName());
        if (!StringUtils.hasText(application.getLabRoomCode())) {
            return roomName;
        }
        return roomName + "（" + application.getLabRoomCode() + "）";
    }

    private String usageTypeName(Integer usageType) {
        if (usageType == null) {
            return "-";
        }
        return switch (usageType) {
            case 1 -> "教学";
            case 2 -> "科研";
            case 3 -> "竞赛";
            case 4 -> "培训";
            case 5 -> "其他";
            default -> "其他";
        };
    }

    private String approvalResultName(Integer approvalResult) {
        if (approvalResult == null) {
            return "-";
        }
        return switch (approvalResult) {
            case 1 -> "通过";
            case 2 -> "拒绝";
            default -> "-";
        };
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String fallbackValue(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
