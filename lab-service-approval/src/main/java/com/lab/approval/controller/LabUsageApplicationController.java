package com.lab.approval.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.ApprovalProcessRequest;
import com.lab.approval.dto.CreateLabUsageApplicationRequest;
import com.lab.approval.dto.LabUsageApplicationDTO;
import com.lab.approval.dto.LabUsageScheduleDTO;
import com.lab.approval.service.LabUsageApplicationService;
import com.lab.approval.support.RequestUserContextResolver;
import com.lab.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lab-usage-applications")
@RequiredArgsConstructor
@Tag(name = "Lab usage applications", description = "Lab usage application and approval APIs")
public class LabUsageApplicationController {

    private final LabUsageApplicationService labUsageApplicationService;
    private final RequestUserContextResolver requestUserContextResolver;

    @PostMapping
    @Operation(summary = "Create lab usage application")
    public Result<Long> createApplication(@RequestBody CreateLabUsageApplicationRequest request,
                                          HttpServletRequest httpRequest) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        return Result.success(labUsageApplicationService.createApplication(request, currentUser));
    }

    @GetMapping
    @Operation(summary = "List lab usage applications")
    public Result<Page<LabUsageApplicationDTO>> listApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long labRoomId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            HttpServletRequest httpRequest
    ) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        return Result.success(labUsageApplicationService.listApplications(
                page, size, status, labRoomId, keyword, startTime, endTime, currentUser
        ));
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending lab usage approvals")
    public Result<List<LabUsageApplicationDTO>> listPendingApplications(HttpServletRequest httpRequest) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        return Result.success(labUsageApplicationService.listPendingApplications(currentUser));
    }

    @GetMapping("/overlaps")
    @Operation(summary = "List overlapping lab usage applications")
    public Result<List<LabUsageApplicationDTO>> listOverlapApplications(
            @RequestParam Long labRoomId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            HttpServletRequest httpRequest
    ) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        return Result.success(labUsageApplicationService.listOverlapApplications(
                labRoomId, startTime, endTime, currentUser
        ));
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Export lab usage application PDF")
    public ResponseEntity<byte[]> exportApplicationPdf(@PathVariable Long id,
                                                       HttpServletRequest httpRequest) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        LabUsageApplicationDTO application = labUsageApplicationService.getApplicationDetail(id, currentUser);
        byte[] pdfBytes = labUsageApplicationService.generateApplicationPdf(id, currentUser);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(
                "attachment",
                "lab_usage_application_" + (application.getApplicationNo() != null ? application.getApplicationNo() : id) + ".pdf"
        );
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lab usage application detail")
    public Result<LabUsageApplicationDTO> getApplicationDetail(@PathVariable Long id,
                                                              HttpServletRequest httpRequest) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        return Result.success(labUsageApplicationService.getApplicationDetail(id, currentUser));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel lab usage application")
    public Result<Void> cancelApplication(@PathVariable Long id,
                                          HttpServletRequest httpRequest) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        labUsageApplicationService.cancelApplication(id, currentUser);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve lab usage application")
    public Result<Void> approveApplication(@PathVariable Long id,
                                           @RequestBody ApprovalProcessRequest request,
                                           HttpServletRequest httpRequest) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        labUsageApplicationService.processApproval(id, request, currentUser);
        return Result.success();
    }

    @GetMapping("/schedules")
    @Operation(summary = "List approved lab usage schedules")
    public Result<List<LabUsageScheduleDTO>> listSchedules(
            @RequestParam(required = false) Long labRoomId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            HttpServletRequest httpRequest
    ) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        return Result.success(labUsageApplicationService.listApprovedSchedules(
                labRoomId, teacherId, startTime, endTime, currentUser
        ));
    }
}
