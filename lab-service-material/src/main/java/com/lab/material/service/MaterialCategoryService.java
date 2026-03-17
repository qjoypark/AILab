package com.lab.material.service;

import com.lab.material.dto.MaterialCategoryDTO;
import com.lab.material.entity.MaterialCategory;

import java.util.List;

/**
 * 药品分类服务接口
 */
public interface MaterialCategoryService {
    
    /**
     * 查询分类树
     */
    List<MaterialCategoryDTO> getCategoryTree();
    
    /**
     * 创建分类
     */
    MaterialCategory createCategory(MaterialCategory category);
    
    /**
     * 更新分类
     */
    MaterialCategory updateCategory(Long id, MaterialCategory category);
    
    /**
     * 删除分类
     */
    void deleteCategory(Long id);
    
    /**
     * 根据ID查询分类
     */
    MaterialCategory getCategoryById(Long id);
}
