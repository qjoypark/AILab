package com.lab.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Simplified lab usage application DTO used by the inventory dashboard todo aggregator.
 */
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

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private Integer approvalStatus;

    private LocalDateTime createdTime;
}
