package com.lab.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("lab_usage_application")
public class LabUsageApplication {

    @TableId(type = IdType.AUTO)
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

    private String remark;

    private Long createdBy;

    private LocalDateTime createdTime;

    private Long updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String currentApproverName;

    @TableField(exist = false)
    private List<Long> currentApproverIds;

    @TableField(exist = false)
    private List<String> currentApproverNames;

    @TableField(exist = false)
    private String currentPendingStatus;
}
