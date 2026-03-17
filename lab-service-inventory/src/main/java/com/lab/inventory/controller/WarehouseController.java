package com.lab.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.inventory.dto.WarehouseDTO;
import com.lab.inventory.entity.Warehouse;
import com.lab.inventory.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 仓库管理控制器
 */
@Tag(name = "仓库管理")
@RestController
@RequestMapping("/api/v1/inventory/warehouses")
@RequiredArgsConstructor
public class WarehouseController {
    
    private final WarehouseService warehouseService;
    
    @Operation(summary = "查询仓库列表")
    @GetMapping
    public Result<Page<Warehouse>> listWarehouses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer warehouseType) {
        Page<Warehouse> result = warehouseService.listWarehouses(page, size, warehouseType);
        return Result.success(result);
    }
    
    @Operation(summary = "查询仓库详情")
    @GetMapping("/{id}")
    public Result<Warehouse> getWarehouse(@PathVariable Long id) {
        Warehouse warehouse = warehouseService.getWarehouseById(id);
        return Result.success(warehouse);
    }
    
    @Operation(summary = "创建仓库")
    @PostMapping
    public Result<Warehouse> createWarehouse(@Validated @RequestBody WarehouseDTO dto) {
        Warehouse warehouse = warehouseService.createWarehouse(dto);
        return Result.success(warehouse);
    }
    
    @Operation(summary = "更新仓库")
    @PutMapping("/{id}")
    public Result<Warehouse> updateWarehouse(@PathVariable Long id, @Validated @RequestBody WarehouseDTO dto) {
        Warehouse warehouse = warehouseService.updateWarehouse(id, dto);
        return Result.success(warehouse);
    }
    
    @Operation(summary = "删除仓库")
    @DeleteMapping("/{id}")
    public Result<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return Result.success();
    }
}
