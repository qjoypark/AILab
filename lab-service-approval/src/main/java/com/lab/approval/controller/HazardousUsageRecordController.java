package com.lab.approval.controller;

import com.lab.approval.dto.HazardousReturnRequest;
import com.lab.approval.dto.HazardousUsageRecordDTO;
import com.lab.approval.entity.HazardousUsageRecord;
import com.lab.approval.service.HazardousUsageRecordService;
import com.lab.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

/**
 * 危化品使用记录控制器
 */
@RestController
@RequestMapping("/api/v1/hazardous/usage-records")
@RequiredArgsConstructor
@Tag(name = "危化品使用记录管理", description = "危化品使用记录相关接口")
public class HazardousUsageRecordController {
    
    private final HazardousUsageRecordService usageRecordService;
    
    @PostMapping
    @Operation(summary = "创建危化品使用记录")
    public Result<HazardousUsageRecord> createUsageRecord(@RequestBody HazardousUsageRecordDTO dto) {
        HazardousUsageRecord record = new HazardousUsageRecord();
        BeanUtils.copyProperties(dto, record);
        
        // 设置初始状态为"使用中"
        record.setStatus(1);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        
        HazardousUsageRecord created = usageRecordService.createUsageRecord(record);
        return Result.success(created);
    }
    
    @PostMapping("/{id}/return")
    @Operation(summary = "危化品归还")
    public Result<HazardousUsageRecord> returnHazardousMaterial(
            @PathVariable Long id,
            @Valid @RequestBody HazardousReturnRequest returnRequest) {
        HazardousUsageRecord updated = usageRecordService.returnHazardousMaterial(id, returnRequest);
        return Result.success(updated);
    }
    
    @GetMapping("/unreturned/{materialId}")
    @Operation(summary = "获取危化品已领用未归还数量")
    public Result<java.math.BigDecimal> getUnreturnedQuantity(@PathVariable Long materialId) {
        java.math.BigDecimal quantity = usageRecordService.getUnreturnedQuantity(materialId);
        return Result.success(quantity);
    }
}
