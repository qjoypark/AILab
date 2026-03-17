package com.lab.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.inventory.dto.StockOutDTO;
import com.lab.inventory.entity.StockOut;

/**
 * 出库服务接口
 */
public interface StockOutService {
    
    /**
     * 分页查询出库单列表
     */
    Page<StockOut> listStockOut(int page, int size, Long warehouseId, Integer status);
    
    /**
     * 根据ID查询出库单详情
     */
    StockOut getStockOutById(Long id);
    
    /**
     * 创建出库单
     */
    StockOut createStockOut(StockOutDTO dto);
    
    /**
     * 确认出库（使用FIFO策略）
     */
    void confirmStockOut(Long id);
    
    /**
     * 取消出库单
     */
    void cancelStockOut(Long id);
    
    /**
     * 根据申请单创建出库单
     * 
     * @param applicationId 申请单ID
     * @return 出库单
     */
    StockOut createStockOutFromApplication(Long applicationId);
}
