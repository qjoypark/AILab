package com.lab.material.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.result.Result;
import com.lab.material.dto.MaterialDTO;
import com.lab.material.entity.Material;
import com.lab.material.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 药品信息控制器
 */
@Tag(name = "药品信息管理")
@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {
    
    private final MaterialService materialService;
    
    @Operation(summary = "分页查询药品列表")
    @GetMapping
    public Result<Page<MaterialDTO>> getMaterialPage(
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
        @Parameter(description = "药品类型") @RequestParam(required = false) Integer materialType,
        @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
        @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId
    ) {
        return Result.success(materialService.getMaterialPage(page, size, materialType, keyword, categoryId));
    }
    
    @Operation(summary = "查询药品详情")
    @GetMapping("/{id}")
    public Result<MaterialDTO> getMaterialById(@PathVariable Long id) {
        return Result.success(materialService.getMaterialById(id));
    }
    
    @Operation(summary = "创建药品")
    @PostMapping
    public Result<Material> createMaterial(@RequestBody Material material) {
        return Result.success(materialService.createMaterial(material));
    }
    
    @Operation(summary = "更新药品")
    @PutMapping("/{id}")
    public Result<Material> updateMaterial(@PathVariable Long id, @RequestBody Material material) {
        return Result.success(materialService.updateMaterial(id, material));
    }
    
    @Operation(summary = "删除药品")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return Result.success();
    }
    
    @Operation(summary = "查询所有危化品列表")
    @GetMapping("/hazardous")
    public Result<java.util.List<MaterialDTO>> getHazardousMaterials() {
        return Result.success(materialService.getHazardousMaterials());
    }
}
