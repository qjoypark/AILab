package com.lab.inventory.client;

import com.lab.inventory.dto.MaterialCategoryInfo;
import com.lab.inventory.dto.MaterialInfo;

import java.util.List;

/**
 * 药品服务客户端接口
 * TODO: 后续使用Feign实现服务间调用
 */
public interface MaterialClient {
    
    /**
     * 获取药品信息
     * 
     * @param materialId 药品ID
     * @return 药品信息
     */
    MaterialInfo getMaterialInfo(Long materialId);
    
    /**
     * 获取所有危化品列表
     * 
     * @return 危化品列表
     */
    List<MaterialInfo> getHazardousMaterials();

    /**
     * 按药品编码/名称关键词查询药品ID
     *
     * @param keyword 关键词
     * @return 匹配到的药品ID列表
     */
    List<Long> searchMaterialIdsByKeyword(String keyword);
    
    /**
     * 获取分类信息
     * 
     * @param categoryId 分类ID
     * @return 分类信息
     */
    MaterialCategoryInfo getCategoryInfo(Long categoryId);
}
