package com.lab.approval.dto;

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
     * 药品ID（别名）
     */
    private Long materialId;
    
    /**
     * 药品名称
     */
    private String materialName;
    
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
     * 是否管控: 0-否, 1-易制毒, 2-易制爆
     */
    private Integer isControlled;
}
