package com.lab.material.controller;

import com.lab.common.result.Result;
import com.lab.material.dto.MaterialCategoryDTO;
import com.lab.material.entity.MaterialCategory;
import com.lab.material.service.MaterialCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 药品分类控制器
 */
@Tag(name = "药品分类管理")
@RestController
@RequestMapping("/api/v1/material-categories")
@RequiredArgsConstructor
public class MaterialCategoryController {
    
    private final MaterialCategoryService categoryService;
    
    @Operation(summary = "查询分类树")
    @GetMapping("/tree")
    public Result<List<MaterialCategoryDTO>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree());
    }
    
    @Operation(summary = "创建分类")
    @PostMapping
    public Result<MaterialCategory> createCategory(@RequestBody MaterialCategory category) {
        return Result.success(categoryService.createCategory(category));
    }
    
    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<MaterialCategory> updateCategory(@PathVariable Long id, @RequestBody MaterialCategory category) {
        return Result.success(categoryService.updateCategory(id, category));
    }
    
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
