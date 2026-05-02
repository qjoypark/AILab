package com.lab.approval.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.LabRoomDTO;
import com.lab.approval.dto.LabRoomForm;
import com.lab.approval.dto.LabRoomManagerDTO;
import com.lab.approval.support.RequestUserContextResolver;

import java.util.List;

public interface LabRoomService {

    Page<LabRoomDTO> listLabRooms(int page,
                                  int size,
                                  Integer status,
                                  Integer roomType,
                                  String keyword,
                                  RequestUserContextResolver.CurrentUser currentUser);

    LabRoomDTO getLabRoom(Long id, RequestUserContextResolver.CurrentUser currentUser);

    LabRoomDTO createLabRoom(LabRoomForm form, RequestUserContextResolver.CurrentUser currentUser);

    LabRoomDTO updateLabRoom(Long id, LabRoomForm form, RequestUserContextResolver.CurrentUser currentUser);

    void deleteLabRoom(Long id, RequestUserContextResolver.CurrentUser currentUser);

    List<LabRoomManagerDTO> listManagers(Long labRoomId, RequestUserContextResolver.CurrentUser currentUser);

    List<LabRoomManagerDTO> saveManagers(Long labRoomId,
                                         List<LabRoomManagerDTO> managers,
                                         RequestUserContextResolver.CurrentUser currentUser);
}
