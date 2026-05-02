package com.lab.approval.dto;

import com.lab.approval.entity.LabRoomManager;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LabRoomManagerDTO {

    private Long id;

    private Long labRoomId;

    private Long managerId;

    private String managerName;

    private Integer isPrimary;

    private Integer status;

    private LocalDateTime createdTime;

    public static LabRoomManagerDTO fromEntity(LabRoomManager manager) {
        LabRoomManagerDTO dto = new LabRoomManagerDTO();
        if (manager == null) {
            return dto;
        }
        dto.setId(manager.getId());
        dto.setLabRoomId(manager.getLabRoomId());
        dto.setManagerId(manager.getManagerId());
        dto.setManagerName(manager.getManagerName());
        dto.setIsPrimary(manager.getIsPrimary());
        dto.setStatus(manager.getStatus());
        dto.setCreatedTime(manager.getCreatedTime());
        return dto;
    }
}
