package com.lab.approval.dto;

import com.lab.approval.entity.LabUsageParticipant;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LabUsageParticipantDTO {

    private Long id;

    private Long applicationId;

    private Long userId;

    private String realName;

    private String deptName;

    private LocalDateTime createdTime;

    public static LabUsageParticipantDTO fromEntity(LabUsageParticipant participant) {
        if (participant == null) {
            return null;
        }
        LabUsageParticipantDTO dto = new LabUsageParticipantDTO();
        dto.setId(participant.getId());
        dto.setApplicationId(participant.getApplicationId());
        dto.setUserId(participant.getUserId());
        dto.setRealName(participant.getRealName());
        dto.setDeptName(participant.getDeptName());
        dto.setCreatedTime(participant.getCreatedTime());
        return dto;
    }
}
