package com.lab.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.inventory.dto.StockCheckDTO;
import com.lab.inventory.entity.StockCheck;
import com.lab.inventory.service.StockCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 库存盘点控制器
 */
@Tag(name = "库存盘点")
@RestController
@RequestMapping("/api/v1/inventory/stock-check")
@RequiredArgsConstructor
public class StockCheckController {
    
    private final StockCheckService stockCheckService;
    
    @Operation(summary = "查询盘点单列表")
    @GetMapping
    public Result<Page<StockCheck>> listStockCheck(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer status) {
        Page<StockCheck> result = stockCheckService.listStockCheck(page, size, keyword, warehouseId, status);
        return Result.success(result);
    }
    
    @Operation(summary = "查询盘点单详情")
    @GetMapping("/{id}")
    public Result<StockCheck> getStockCheck(@PathVariable Long id) {
        StockCheck stockCheck = stockCheckService.getStockCheckById(id);
        return Result.success(stockCheck);
    }
    
    @Operation(summary = "创建盘点单")
    @PostMapping
    public Result<StockCheck> createStockCheck(@Validated @RequestBody StockCheckDTO dto) {
        StockCheck stockCheck = stockCheckService.createStockCheck(dto);
        return Result.success(stockCheck);
    }
    
    @Operation(summary = "提交盘点明细")
    @PostMapping("/{id}/items")
    public Result<Void> submitCheckDetails(@PathVariable Long id, @RequestBody StockCheckDTO dto) {
        stockCheckService.submitCheckDetails(id, dto);
        return Result.success();
    }
    
    @Operation(summary = "完成盘点")
    @PostMapping("/{id}/complete")
    public Result<Void> completeStockCheck(@PathVariable Long id) {
        stockCheckService.completeStockCheck(id);
        return Result.success();
    }
}
