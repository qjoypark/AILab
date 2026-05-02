package com.lab.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LabUsageScheduleDTO {

    private Long applicationId;

    private String applicationNo;

    private Long labRoomId;

    private String labRoomCode;

    private String labRoomName;

    private String applicantName;

    private String applicantDept;

    private String usagePurpose;

    private String projectName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private List<LabUsageParticipantDTO> participants;
}
