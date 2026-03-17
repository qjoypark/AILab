package com.lab.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.inventory.dto.AlertRecordDTO;

/**
 * 预警服务接口
 */
public interface AlertService {
    
    /**
     * 创建预警记录
     */
    Long createAlert(Integer alertType, Integer alertLevel, String businessType, 
                     Long businessId, String title, String content);
    
    /**
     * 查询预警列表
     */
    Page<AlertRecordDTO> listAlerts(int page, int size, Integer alertType, 
                                     Integer alertLevel, Integer status);
    
    /**
     * 查询预警详情
     */
    AlertRecordDTO getAlert(Long id);
    
    /**
     * 处理预警
     */
    void handleAlert(Long id, Long handlerId, String handleRemark);
    
    /**
     * 忽略预警
     */
    void ignoreAlert(Long id, Long handlerId);
    
    /**
     * 检查低库存预警
     */
    void checkLowStockAlert();
    
    /**
     * 检查有效期预警
     */
    void checkExpirationAlert();
}
