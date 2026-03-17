package com.lab.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.inventory.dto.StockOutDTO;
import com.lab.inventory.entity.StockOut;
import com.lab.inventory.service.StockOutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 出库管理控制器
 */
@Tag(name = "出库管理")
@RestController
@RequestMapping("/api/v1/inventory/stock-out")
@RequiredArgsConstructor
public class StockOutController {
    
    private final StockOutService stockOutService;
    
    @Operation(summary = "查询出库单列表")
    @GetMapping
    public Result<Page<StockOut>> listStockOut(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer status) {
        Page<StockOut> result = stockOutService.listStockOut(page, size, warehouseId, status);
        return Result.success(result);
    }
    
    @Operation(summary = "查询出库单详情")
    @GetMapping("/{id}")
    public Result<StockOut> getStockOut(@PathVariable Long id) {
        StockOut stockOut = stockOutService.getStockOutById(id);
        return Result.success(stockOut);
    }
    
    @Operation(summary = "创建出库单")
    @PostMapping
    public Result<StockOut> createStockOut(@Validated @RequestBody StockOutDTO dto) {
        StockOut stockOut = stockOutService.createStockOut(dto);
        return Result.success(stockOut);
    }
    
    @Operation(summary = "根据申请单创建出库单")
    @PostMapping("/from-application")
    public Result<StockOut> createStockOutFromApplication(@RequestBody Map<String, Long> request) {
        Long applicationId = request.get("applicationId");
        if (applicationId == null) {
            return Result.error(400, "申请单ID不能为空");
        }
        StockOut stockOut = stockOutService.createStockOutFromApplication(applicationId);
        return Result.success(stockOut);
    }
    
    @Operation(summary = "确认出库")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirmStockOut(@PathVariable Long id) {
        stockOutService.confirmStockOut(id);
        return Result.success();
    }
    
    @Operation(summary = "取消出库单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelStockOut(@PathVariable Long id) {
        stockOutService.cancelStockOut(id);
        return Result.success();
    }
}
