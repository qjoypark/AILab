package com.lab.approval.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.CreateApplicationRequest;
import com.lab.approval.dto.MaterialApplicationDTO;
import com.lab.approval.entity.MaterialApplication;
import com.lab.approval.service.MaterialApplicationService;
import com.lab.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
    
    @PostMapping
    @Operation(summary = "创建领用申请", description = "创建新的领用申请，自动检查库存并启动审批流程")
    public Result<Long> createApplication(@RequestBody CreateApplicationRequest request) {
        log.info("接收创建申请请求: applicationType={}", request.getApplicationType());
        
        // TODO: 从JWT token或session中获取当前用户信息
        // 临时使用模拟数据
        Long applicantId = 1L;
        String applicantName = "测试用户";
        String applicantDept = "测试部门";
        
        Long applicationId = applicationService.createApplication(
            request, 
            applicantId, 
            applicantName, 
            applicantDept
        );
        
        return Result.success(applicationId);
    }
    
    @GetMapping
    @Operation(summary = "查询申请列表", description = "分页查询领用申请列表，支持多条件筛选")
    public Result<Page<MaterialApplication>> listApplications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "申请类型") @RequestParam(required = false) Integer applicationType,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("查询申请列表: page={}, size={}, status={}, applicationType={}", 
            page, size, status, applicationType);
        
        Page<MaterialApplication> result = applicationService.listApplications(
            page, size, status, applicationType, startDate, endDate
        );
        
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "查询申请详情", description = "查询指定申请单的详细信息，包括申请明细和审批记录")
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
            @Parameter(description = "申请单ID") @PathVariable Long id
    ) {
        log.info("取消申请: id={}", id);
        
        // TODO: 从JWT token或session中获取当前用户ID
        Long userId = 1L;
        
        applicationService.cancelApplication(id, userId);
        
        return Result.success();
    }
    
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批申请", description = "审批领用申请，支持修改批准数量，审批通过后流转到下一级或标记为审批完成，审批拒绝后终止流程")
    public Result<Void> approveApplication(
            @Parameter(description = "申请单ID") @PathVariable Long id,
            @RequestBody com.lab.approval.dto.ApprovalProcessRequest request
    ) {
        log.info("审批申请: id={}, result={}", id, request.getApprovalResult());
        
        // TODO: 从JWT token或session中获取当前用户信息
        Long approverId = 2L;
        String approverName = "审批人";
        
        applicationService.processApproval(id, request, approverId, approverName);
        
        return Result.success();
    }
    
    @PutMapping("/{id}/status")
    @Operation(summary = "更新申请单状态", description = "更新申请单状态（内部接口，用于库存服务回调）")
    public Result<Void> updateApplicationStatus(
            @Parameter(description = "申请单ID") @PathVariable Long id,
            @RequestBody java.util.Map<String, Integer> request
    ) {
        Integer status = request.get("status");
        log.info("更新申请单状态: id={}, status={}", id, status);
        
        if (status == null) {
            return Result.error(400, "状态不能为空");
        }
        
        applicationService.updateApplicationStatus(id, status);
        
        return Result.success();
    }
}
