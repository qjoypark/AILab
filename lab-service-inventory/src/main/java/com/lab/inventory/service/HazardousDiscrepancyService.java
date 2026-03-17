package com.lab.inventory.service;

/**
 * 危化品账实差异服务接口
 */
public interface HazardousDiscrepancyService {
    
    /**
     * 计算所有危化品的账实差异
     * 账实差异 = (账面库存 - 实际库存) / 账面库存 × 100%
     * 实际库存 = 账面库存 - 已领用未归还数量
     */
    void calculateDiscrepancy();
}
