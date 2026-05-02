package com.lab.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LabRoomForm {

    @NotBlank(message = "roomCode is required")
    private String roomCode;

    @NotBlank(message = "roomName is required")
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
}
