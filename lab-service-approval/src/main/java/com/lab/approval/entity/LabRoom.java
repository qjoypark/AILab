package com.lab.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_room")
public class LabRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomCode;

    private String roomName;

    private String building;

    private String floor;

    private String roomNo;

    private Integer capacity;

    private Integer roomType;

    private Integer safetyLevel;

    private String equipmentSummary;

    private String notice;

    private Integer status;

    private Long createdBy;

    private LocalDateTime createdTime;

    private Long updatedBy;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
