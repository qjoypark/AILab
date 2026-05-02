package com.lab.approval.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.ApprovalProcessRequest;
import com.lab.approval.dto.CreateLabUsageApplicationRequest;
import com.lab.approval.dto.LabUsageApplicationDTO;
import com.lab.approval.dto.LabUsageScheduleDTO;
import com.lab.approval.support.RequestUserContextResolver;

import java.time.LocalDateTime;
import java.util.List;

public interface LabUsageApplicationService {

    Long createApplication(CreateLabUsageApplicationRequest request,
                           RequestUserContextResolver.CurrentUser currentUser);

    Page<LabUsageApplicationDTO> listApplications(int page,
                                                  int size,
                                                  Integer status,
                                                  Long labRoomId,
                                                  String keyword,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  RequestUserContextResolver.CurrentUser currentUser);

    List<LabUsageApplicationDTO> listPendingApplications(RequestUserContextResolver.CurrentUser currentUser);

    LabUsageApplicationDTO getApplicationDetail(Long id,
                                                RequestUserContextResolver.CurrentUser currentUser);

    byte[] generateApplicationPdf(Long id,
                                  RequestUserContextResolver.CurrentUser currentUser);

    List<LabUsageApplicationDTO> listOverlapApplications(Long labRoomId,
                                                         LocalDateTime startTime,
                                                         LocalDateTime endTime,
                                                         RequestUserContextResolver.CurrentUser currentUser);

    void cancelApplication(Long id, RequestUserContextResolver.CurrentUser currentUser);

    void processApproval(Long id,
                         ApprovalProcessRequest request,
                         RequestUserContextResolver.CurrentUser currentUser);

    List<LabUsageScheduleDTO> listApprovedSchedules(Long labRoomId,
                                                    Long teacherId,
                                                    LocalDateTime startTime,
                                                    LocalDateTime endTime,
                                                    RequestUserContextResolver.CurrentUser currentUser);
}
