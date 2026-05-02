package com.lab.approval.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.approval.dto.HazardousReturnRequest;
import com.lab.approval.dto.HazardousUsageRecordDTO;
import com.lab.approval.entity.HazardousUsageRecord;
import com.lab.approval.service.HazardousUsageRecordService;
import com.lab.approval.support.RequestUserContextResolver;
import com.lab.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Usage record controller (historical API path kept).
 */
@RestController
@RequestMapping("/api/v1/hazardous/usage-records")
@RequiredArgsConstructor
@Tag(name = "Usage Records", description = "Usage and return APIs for claimed materials")
public class HazardousUsageRecordController {

    private final HazardousUsageRecordService usageRecordService;
    private final RequestUserContextResolver requestUserContextResolver;

    @GetMapping
    @Operation(summary = "List usage records")
    public Result<Page<HazardousUsageRecord>> listUsageRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        RequestUserContextResolver.CurrentUser currentUser = requestUserContextResolver.resolve(request);
        Long queryUserId = isPrivilegedUser(currentUser) ? null : currentUser.userId();
        Integer queryStatus = (status != null && status < 0) ? null : status;
        Page<HazardousUsageRecord> pageResult = usageRecordService.listUsageRecords(
                page, size, queryStatus, keyword, queryUserId, startDate, endDate
        );
        return Result.success(pageResult);
    }

    @PostMapping
    @Operation(summary = "Create usage record")
    public Result<HazardousUsageRecord> createUsageRecord(@RequestBody HazardousUsageRecordDTO dto) {
        HazardousUsageRecord record = new HazardousUsageRecord();
        BeanUtils.copyProperties(dto, record);
        record.setStatus(1);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        HazardousUsageRecord created = usageRecordService.createUsageRecord(record);
        return Result.success(created);
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Return material")
    public Result<HazardousUsageRecord> returnHazardousMaterial(
            @PathVariable Long id,
            @Valid @RequestBody HazardousReturnRequest returnRequest) {
        HazardousUsageRecord updated = usageRecordService.returnHazardousMaterial(id, returnRequest);
        return Result.success(updated);
    }

    @GetMapping("/unreturned/{materialId}")
    @Operation(summary = "Get unreturned quantity")
    public Result<BigDecimal> getUnreturnedQuantity(@PathVariable Long materialId) {
        BigDecimal quantity = usageRecordService.getUnreturnedQuantity(materialId);
        return Result.success(quantity);
    }

    private boolean isPrivilegedUser(RequestUserContextResolver.CurrentUser currentUser) {
        if (currentUser == null) {
            return false;
        }
        if (currentUser.hasAnyRole("ADMIN", "CENTER_ADMIN", "LAB_MANAGER")) {
            return true;
        }
        String username = currentUser.username();
        return StringUtils.hasText(username) && "admin".equalsIgnoreCase(username.trim());
    }
}
