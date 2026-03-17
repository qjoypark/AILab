package com.lab.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.exception.BusinessException;
import com.lab.material.dto.MaterialCategoryDTO;
import com.lab.material.entity.MaterialCategory;
import com.lab.material.mapper.MaterialCategoryMapper;
import com.lab.material.service.MaterialCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 药品分类服务实现
 */
@Service
@RequiredArgsConstructor
public class MaterialCategoryServiceImpl implements MaterialCategoryService {
    
    private final MaterialCategoryMapper categoryMapper;
    
    @Override
    public List<MaterialCategoryDTO> getCategoryTree() {
        // 查询所有分类
        List<MaterialCategory> allCategories = categoryMapper.selectList(
            new LambdaQueryWrapper<MaterialCategory>()
                .orderByAsc(MaterialCategory::getSortOrder)
        );
        
        // 构建树形结构
        return buildTree(allCategories, 0L);
    }
    
    private List<MaterialCategoryDTO> buildTree(List<MaterialCategory> categories, Long parentId) {
        List<MaterialCategoryDTO> result = new ArrayList<>();
        
        for (MaterialCategory category : categories) {
            if (category.getParentId().equals(parentId)) {
                MaterialCategoryDTO dto = new MaterialCategoryDTO();
                BeanUtils.copyProperties(category, dto);
                
                // 递归查找子分类
                List<MaterialCategoryDTO> children = buildTree(categories, category.getId());
                if (!children.isEmpty()) {
                    dto.setChildren(children);
                }
                
                result.add(dto);
            }
        }
        
        return result;
    }
    
    @Override
    public MaterialCategory createCategory(MaterialCategory category) {
        // 检查分类编码是否已存在
        Long count = categoryMapper.selectCount(
            new LambdaQueryWrapper<MaterialCategory>()
                .eq(MaterialCategory::getCategoryCode, category.getCategoryCode())
        );
        if (count > 0) {
            throw new BusinessException("分类编码已存在");
        }
        
        categoryMapper.insert(category);
        return category;
    }
    
    @Override
    public MaterialCategory updateCategory(Long id, MaterialCategory category) {
        MaterialCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 检查分类编码是否与其他分类重复
        if (!existing.getCategoryCode().equals(category.getCategoryCode())) {
            Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<MaterialCategory>()
                    .eq(MaterialCategory::getCategoryCode, category.getCategoryCode())
                    .ne(MaterialCategory::getId, id)
            );
            if (count > 0) {
                throw new BusinessException("分类编码已存在");
            }
        }
        
        category.setId(id);
        categoryMapper.updateById(category);
        return category;
    }
    
    @Override
    public void deleteCategory(Long id) {
        // 检查是否有子分类
        Long childCount = categoryMapper.selectCount(
            new LambdaQueryWrapper<MaterialCategory>()
                .eq(MaterialCategory::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }
        
        categoryMapper.deleteById(id);
    }
    
    @Override
    public MaterialCategory getCategoryById(Long id) {
        MaterialCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
    }
}
