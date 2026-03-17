package com.lab.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.inventory.dto.StockAlertConfigDTO;

/**
 * 库存预警配置服务接口
 */
public interface StockAlertConfigService {
    
    /**
     * 创建预警配置
     */
    Long createAlertConfig(StockAlertConfigDTO dto);
    
    /**
     * 更新预警配置
     */
    void updateAlertConfig(Long id, StockAlertConfigDTO dto);
    
    /**
     * 查询预警配置列表
     */
    Page<StockAlertConfigDTO> listAlertConfigs(int page, int size, Long materialId, Integer alertType);
    
    /**
     * 查询预警配置详情
     */
    StockAlertConfigDTO getAlertConfig(Long id);
    
    /**
     * 删除预警配置
     */
    void deleteAlertConfig(Long id);
}
