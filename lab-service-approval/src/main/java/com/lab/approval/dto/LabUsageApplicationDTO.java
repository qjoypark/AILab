package com.lab.approval.dto;

import com.lab.approval.entity.LabUsageApplication;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LabUsageApplicationDTO {

    private Long id;

    private String applicationNo;

    private Long applicantId;

    private String applicantName;

    private String applicantDept;

    private Long labRoomId;

    private String labRoomCode;

    private String labRoomName;

    private Integer usageType;

    private String usagePurpose;

    private String projectName;

    private Integer expectedAttendeeCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String specialEquipment;

    private Integer safetyCommitment;

    private Integer status;

    private Integer approvalStatus;

    private Long currentApproverId;

    private String currentApproverRole;

    private String currentApproverName;

    private List<Long> currentApproverIds;

    private List<String> currentApproverNames;

    private String currentPendingStatus;

    private String remark;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private List<LabUsageParticipantDTO> participants;

    private List<ApprovalRecordDTO> approvalRecords;

    public static LabUsageApplicationDTO fromEntity(LabUsageApplication application) {
        if (application == null) {
            return null;
        }
        LabUsageApplicationDTO dto = new LabUsageApplicationDTO();
        dto.setId(application.getId());
        dto.setApplicationNo(application.getApplicationNo());
        dto.setApplicantId(application.getApplicantId());
        dto.setApplicantName(application.getApplicantName());
        dto.setApplicantDept(application.getApplicantDept());
        dto.setLabRoomId(application.getLabRoomId());
        dto.setLabRoomCode(application.getLabRoomCode());
        dto.setLabRoomName(application.getLabRoomName());
        dto.setUsageType(application.getUsageType());
        dto.setUsagePurpose(application.getUsagePurpose());
        dto.setProjectName(application.getProjectName());
        dto.setExpectedAttendeeCount(application.getExpectedAttendeeCount());
        dto.setStartTime(application.getStartTime());
        dto.setEndTime(application.getEndTime());
        dto.setSpecialEquipment(application.getSpecialEquipment());
        dto.setSafetyCommitment(application.getSafetyCommitment());
        dto.setStatus(application.getStatus());
        dto.setApprovalStatus(application.getApprovalStatus());
        dto.setCurrentApproverId(application.getCurrentApproverId());
        dto.setCurrentApproverRole(application.getCurrentApproverRole());
        dto.setCurrentApproverName(application.getCurrentApproverName());
        dto.setCurrentApproverIds(application.getCurrentApproverIds());
        dto.setCurrentApproverNames(application.getCurrentApproverNames());
        dto.setCurrentPendingStatus(application.getCurrentPendingStatus());
        dto.setRemark(application.getRemark());
        dto.setCreatedTime(application.getCreatedTime());
        dto.setUpdatedTime(application.getUpdatedTime());
        return dto;
    }
}
