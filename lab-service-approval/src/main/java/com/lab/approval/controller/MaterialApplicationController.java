package com.lab.approval.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.ApprovalProcessRequest;
import com.lab.approval.dto.CreateApplicationRequest;
import com.lab.approval.dto.MaterialApplicationDTO;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.service.MaterialApplicationService;
import com.lab.approval.support.RequestUserContextResolver;
import com.lab.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 领用申请控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "领用申请管理", description = "领用申请相关接口")
public class MaterialApplicationController {

    private final MaterialApplicationService applicationService;
    private final RequestUserContextResolver requestUserContextResolver;

    @PostMapping
    @Operation(summary = "创建领用申请", description = "创建新的领用申请，自动检查库存并启动审批流程")
    public Result<Long> createApplication(@RequestBody CreateApplicationRequest request, HttpServletRequest httpRequest) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        log.info("接收创建申请请求: userId={}, applicationType={}", currentUser.userId(), request.getApplicationType());

        Long applicationId = applicationService.createApplication(
                request,
                currentUser.userId(),
                currentUser.displayName(),
                currentUser.department()
        );
        return Result.success(applicationId);
    }

    @GetMapping
    @Operation(summary = "查询申请列表", description = "分页查询领用申请列表，支持多条件筛选")
    public Result<Page<MaterialApplication>> listApplications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "后端状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "申请类型") @RequestParam(required = false) Integer applicationType,
            @Parameter(description = "关键词（申请单号/申请人/部门）") @RequestParam(required = false) String keyword,
            @Parameter(description = "前端状态（1审批中 2审批通过 3审批拒绝 4已出库 5已取消）") @RequestParam(required = false) Integer uiStatus,
            @Parameter(description = "开始日期")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("查询申请列表: page={}, size={}, status={}, applicationType={}, keyword={}, uiStatus={}",
                page, size, status, applicationType, keyword, uiStatus);
        Page<MaterialApplication> result = applicationService.listApplications(
                page, size, status, applicationType, keyword, uiStatus, startDate, endDate
        );
        return Result.success(result);
    }

    @GetMapping("/pending")
    public Result<List<MaterialApplicationDTO>> listPendingApplications(
            @RequestParam(required = false) Long approverId,
            HttpServletRequest httpRequest
    ) {
        Long targetApproverId = approverId;
        if (targetApproverId == null) {
            RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
            targetApproverId = currentUser.userId();
        }
        List<MaterialApplicationDTO> result = applicationService.listPendingApplications(targetApproverId);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询申请详情", description = "查询指定申请单详情，包含申请明细与审批记录")
    public Result<MaterialApplicationDTO> getApplicationDetail(
            @Parameter(description = "申请单ID") @PathVariable Long id
    ) {
        log.info("查询申请详情: id={}", id);
        MaterialApplicationDTO detail = applicationService.getApplicationDetail(id);
        return Result.success(detail);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消申请", description = "取消待审批或审批中的申请")
    public Result<Void> cancelApplication(
            @Parameter(description = "申请单ID") @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        log.info("取消申请: id={}, userId={}", id, currentUser.userId());
        applicationService.cancelApplication(id, currentUser.userId());
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批申请", description = "审批领用申请，支持修改批准数量")
    public Result<Void> approveApplication(
            @Parameter(description = "申请单ID") @PathVariable Long id,
            @RequestBody ApprovalProcessRequest request,
            HttpServletRequest httpRequest
    ) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(httpRequest);
        log.info("审批申请: id={}, userId={}, result={}", id, currentUser.userId(), request.getApprovalResult());
        applicationService.processApproval(id, request, currentUser.userId(), currentUser.displayName());
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新申请状态", description = "更新申请单状态（内部接口）")
    public Result<Void> updateApplicationStatus(
            @Parameter(description = "申请单ID") @PathVariable Long id,
            @RequestBody Map<String, Integer> request
    ) {
        Integer status = request.get("status");
        if (status == null) {
            return Result.error(400, "状态不能为空");
        }
        log.info("更新申请状态: id={}, status={}", id, status);
        applicationService.updateApplicationStatus(id, status);
        return Result.success();
    }
}
