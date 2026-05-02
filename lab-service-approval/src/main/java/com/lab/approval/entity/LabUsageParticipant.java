package com.lab.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_usage_participant")
public class LabUsageParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private Long userId;

    private String realName;

    private String deptName;

    private LocalDateTime createdTime;
}
