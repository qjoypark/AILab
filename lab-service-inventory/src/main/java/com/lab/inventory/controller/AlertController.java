package com.lab.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.inventory.dto.AlertRecordDTO;
import com.lab.inventory.dto.StockAlertConfigDTO;
import com.lab.inventory.service.AlertService;
import com.lab.inventory.service.StockAlertConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 预警管理控制器
 */
@Tag(name = "预警管理")
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {
    
    private final AlertService alertService;
    private final StockAlertConfigService alertConfigService;
    
    @Operation(summary = "查询预警列表")
    @GetMapping
    public Result<Page<AlertRecordDTO>> listAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer alertType,
            @RequestParam(required = false) Integer alertLevel,
            @RequestParam(required = false) Integer status) {
        Page<AlertRecordDTO> result = alertService.listAlerts(page, size, alertType, alertLevel, status);
        return Result.success(result);
    }
    
    @Operation(summary = "查询预警详情")
    @GetMapping("/{id}")
    public Result<AlertRecordDTO> getAlert(@PathVariable Long id) {
        AlertRecordDTO alert = alertService.getAlert(id);
        return Result.success(alert);
    }
    
    @Operation(summary = "处理预警")
    @PostMapping("/{id}/handle")
    public Result<Void> handleAlert(
            @PathVariable Long id,
            @RequestParam Long handlerId,
            @RequestParam(required = false) String handleRemark) {
        alertService.handleAlert(id, handlerId, handleRemark);
        return Result.success();
    }
    
    @Operation(summary = "忽略预警")
    @PostMapping("/{id}/ignore")
    public Result<Void> ignoreAlert(
            @PathVariable Long id,
            @RequestParam Long handlerId) {
        alertService.ignoreAlert(id, handlerId);
        return Result.success();
    }
    
    @Operation(summary = "创建预警配置")
    @PostMapping("/config")
    public Result<Long> createAlertConfig(@RequestBody StockAlertConfigDTO dto) {
        Long id = alertConfigService.createAlertConfig(dto);
        return Result.success(id);
    }
    
    @Operation(summary = "更新预警配置")
    @PutMapping("/config/{id}")
    public Result<Void> updateAlertConfig(
            @PathVariable Long id,
            @RequestBody StockAlertConfigDTO dto) {
        alertConfigService.updateAlertConfig(id, dto);
        return Result.success();
    }
    
    @Operation(summary = "查询预警配置列表")
    @GetMapping("/config")
    public Result<Page<StockAlertConfigDTO>> listAlertConfigs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Integer alertType) {
        Page<StockAlertConfigDTO> result = alertConfigService.listAlertConfigs(page, size, materialId, alertType);
        return Result.success(result);
    }
    
    @Operation(summary = "查询预警配置详情")
    @GetMapping("/config/{id}")
    public Result<StockAlertConfigDTO> getAlertConfig(@PathVariable Long id) {
        StockAlertConfigDTO config = alertConfigService.getAlertConfig(id);
        return Result.success(config);
    }
    
    @Operation(summary = "删除预警配置")
    @DeleteMapping("/config/{id}")
    public Result<Void> deleteAlertConfig(@PathVariable Long id) {
        alertConfigService.deleteAlertConfig(id);
        return Result.success();
    }
}
