package com.lab.material.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.material.dto.SupplierDTO;
import com.lab.material.entity.Supplier;
import com.lab.material.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 供应商控制器
 */
@Tag(name = "供应商管理")
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    
    private final SupplierService supplierService;
    
    @Operation(summary = "分页查询供应商列表")
    @GetMapping
    public Result<Page<SupplierDTO>> getSupplierPage(
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
        @Parameter(description = "关键词") @RequestParam(required = false) String keyword
    ) {
        return Result.success(supplierService.getSupplierPage(page, size, keyword));
    }
    
    @Operation(summary = "查询供应商详情")
    @GetMapping("/{id}")
    public Result<SupplierDTO> getSupplierById(@PathVariable Long id) {
        return Result.success(supplierService.getSupplierById(id));
    }
    
    @Operation(summary = "创建供应商")
    @PostMapping
    public Result<Supplier> createSupplier(@RequestBody Supplier supplier) {
        return Result.success(supplierService.createSupplier(supplier));
    }
    
    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    public Result<Supplier> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        return Result.success(supplierService.updateSupplier(id, supplier));
    }
    
    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return Result.success();
    }
}
