package com.lab.inventory.dto;

import lombok.Data;

/**
 * 药品信息DTO
 */
@Data
public class MaterialInfo {
    
    /**
     * 药品ID
     */
    private Long id;
    
    /**
     * 药品名称
     */
    private String materialName;
    
    /**
     * 药品编码
     */
    private String materialCode;
    
    /**
     * 规格
     */
    private String specification;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 药品类型: 1-耗材, 2-试剂, 3-危化品
     */
    private Integer materialType;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 是否管控: 0-否, 1-易制毒, 2-易制爆
     */
    private Integer isControlled;
    
    /**
     * CAS号
     */
    private String casNumber;
    
    /**
     * 危险类别
     */
    private String dangerCategory;
}
