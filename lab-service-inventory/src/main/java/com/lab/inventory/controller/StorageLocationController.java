package com.lab.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.inventory.dto.StorageLocationDTO;
import com.lab.inventory.entity.StorageLocation;
import com.lab.inventory.service.StorageLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存储位置管理控制器
 */
@Tag(name = "存储位置管理")
@RestController
@RequestMapping("/api/v1/inventory/storage-locations")
@RequiredArgsConstructor
public class StorageLocationController {
    
    private final StorageLocationService storageLocationService;
    
    @Operation(summary = "查询存储位置列表")
    @GetMapping
    public Result<Page<StorageLocation>> listStorageLocations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long warehouseId) {
        Page<StorageLocation> result = storageLocationService.listStorageLocations(page, size, warehouseId);
        return Result.success(result);
    }
    
    @Operation(summary = "根据仓库ID查询所有存储位置")
    @GetMapping("/by-warehouse/{warehouseId}")
    public Result<List<StorageLocation>> listByWarehouseId(@PathVariable Long warehouseId) {
        List<StorageLocation> result = storageLocationService.listByWarehouseId(warehouseId);
        return Result.success(result);
    }
    
    @Operation(summary = "查询存储位置详情")
    @GetMapping("/{id}")
    public Result<StorageLocation> getStorageLocation(@PathVariable Long id) {
        StorageLocation location = storageLocationService.getStorageLocationById(id);
        return Result.success(location);
    }
    
    @Operation(summary = "创建存储位置")
    @PostMapping
    public Result<StorageLocation> createStorageLocation(@Validated @RequestBody StorageLocationDTO dto) {
        StorageLocation location = storageLocationService.createStorageLocation(dto);
        return Result.success(location);
    }
    
    @Operation(summary = "更新存储位置")
    @PutMapping("/{id}")
    public Result<StorageLocation> updateStorageLocation(@PathVariable Long id, @Validated @RequestBody StorageLocationDTO dto) {
        StorageLocation location = storageLocationService.updateStorageLocation(id, dto);
        return Result.success(location);
    }
    
    @Operation(summary = "删除存储位置")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStorageLocation(@PathVariable Long id) {
        storageLocationService.deleteStorageLocation(id);
        return Result.success();
    }
}
