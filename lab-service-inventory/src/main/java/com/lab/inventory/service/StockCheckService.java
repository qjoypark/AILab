package com.lab.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.inventory.dto.StockCheckDTO;
import com.lab.inventory.entity.StockCheck;

/**
 * 库存盘点服务接口
 */
public interface StockCheckService {
    
    /**
     * 分页查询盘点单列表
     */
    Page<StockCheck> listStockCheck(int page, int size, String keyword, Long warehouseId, Integer status);
    
    /**
     * 根据ID查询盘点单详情
     */
    StockCheck getStockCheckById(Long id);
    
    /**
     * 创建盘点单
     */
    StockCheck createStockCheck(StockCheckDTO dto);
    
    /**
     * 提交盘点明细
     */
    void submitCheckDetails(Long id, StockCheckDTO dto);
    
    /**
     * 完成盘点（调整库存）
     */
    void completeStockCheck(Long id);
}
