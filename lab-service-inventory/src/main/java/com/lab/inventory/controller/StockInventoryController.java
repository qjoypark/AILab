package com.lab.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.service.StockInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存查询控制器
 */
@Tag(name = "库存查询")
@RestController
@RequestMapping("/api/v1/inventory/stock")
@RequiredArgsConstructor
public class StockInventoryController {
    
    private final StockInventoryService stockInventoryService;
    
    @Operation(summary = "查询库存列表")
    @GetMapping
    public Result<Page<StockInventory>> listStock(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Boolean lowStock) {
        Page<StockInventory> result = stockInventoryService.listStock(
                page,
                size,
                materialId,
                keyword,
                warehouseId,
                lowStock
        );
        return Result.success(result);
    }
    
    @Operation(summary = "查询指定药品的库存明细")
    @GetMapping("/{materialId}/detail")
    public Result<List<StockInventory>> getStockDetail(@PathVariable Long materialId) {
        List<StockInventory> result = stockInventoryService.getStockDetailByMaterialId(materialId);
        return Result.success(result);
    }
}
