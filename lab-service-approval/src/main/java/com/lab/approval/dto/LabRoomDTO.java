package com.lab.approval.dto;

import com.lab.approval.entity.LabRoom;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class LabRoomDTO {

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

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private List<LabRoomManagerDTO> managers = new ArrayList<>();

    public static LabRoomDTO fromEntity(LabRoom room) {
        LabRoomDTO dto = new LabRoomDTO();
        if (room == null) {
            return dto;
        }
        dto.setId(room.getId());
        dto.setRoomCode(room.getRoomCode());
        dto.setRoomName(room.getRoomName());
        dto.setBuilding(room.getBuilding());
        dto.setFloor(room.getFloor());
        dto.setRoomNo(room.getRoomNo());
        dto.setCapacity(room.getCapacity());
        dto.setRoomType(room.getRoomType());
        dto.setSafetyLevel(room.getSafetyLevel());
        dto.setEquipmentSummary(room.getEquipmentSummary());
        dto.setNotice(room.getNotice());
        dto.setStatus(room.getStatus());
        dto.setCreatedTime(room.getCreatedTime());
        dto.setUpdatedTime(room.getUpdatedTime());
        return dto;
    }
}
