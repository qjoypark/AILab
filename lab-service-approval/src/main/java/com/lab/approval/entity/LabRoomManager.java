package com.lab.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_room_manager")
public class LabRoomManager {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long labRoomId;

    private Long managerId;

    private String managerName;

    private Integer isPrimary;

    private Integer status;

    private Long createdBy;

    private LocalDateTime createdTime;
}
