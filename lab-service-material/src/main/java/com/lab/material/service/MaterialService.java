package com.lab.material.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.material.dto.MaterialDTO;
import com.lab.material.entity.Material;

import java.util.List;

/**
 * 药品信息服务接口
 */
public interface MaterialService {
    
    /**
     * 分页查询药品列表
     */
    Page<MaterialDTO> getMaterialPage(int page, int size, Integer materialType, Integer isControlled, String keyword, Long categoryId);
    
    /**
     * 根据ID查询药品详情
     */
    MaterialDTO getMaterialById(Long id);
    
    /**
     * 创建药品
     */
    Material createMaterial(Material material);
    
    /**
     * 更新药品
     */
    Material updateMaterial(Long id, Material material);
    
    /**
     * 删除药品
     */
    void deleteMaterial(Long id);
    
    /**
     * 查询所有危化品列表
     * 危化品定义：materialType=3 或 isControlled>0
     */
    List<MaterialDTO> getHazardousMaterials();
}
