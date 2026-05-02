package com.lab.approval.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SaveLabRoomManagersRequest {

    private List<LabRoomManagerDTO> managers = new ArrayList<>();
}
