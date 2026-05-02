package com.lab.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateLabUsageApplicationRequest {

    private Long labRoomId;

    private Integer usageType;

    private String usagePurpose;

    private String projectName;

    private Integer expectedAttendeeCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String specialEquipment;

    private Integer safetyCommitment;

    private String remark;

    private List<ParticipantRequest> participants;

    @Data
    public static class ParticipantRequest {
        private Long userId;
        private String realName;
        private String deptName;
    }
}
