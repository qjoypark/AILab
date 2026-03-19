package com.lab.approval.dto;

import lombok.Data;

/**
 * 审批候选人DTO
 */
@Data
public class ApproverCandidateDTO {

    private Long userId;

    private String username;

    private String realName;

    private String roleCode;

    public String getDisplayName() {
        if (realName != null && !realName.trim().isEmpty()) {
            return realName;
        }
        return username;
    }
}
