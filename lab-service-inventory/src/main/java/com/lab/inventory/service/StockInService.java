package com.lab.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.inventory.dto.StockInDTO;
import com.lab.inventory.entity.StockIn;

import java.time.LocalDateTime;

/**
 * 入库服务接口
 */
public interface StockInService {
    
    /**
     * 分页查询入库单列表
     */
    Page<StockIn> listStockIn(
            int page,
            int size,
            Long warehouseId,
            Integer status,
            LocalDateTime createdTimeStart,
            LocalDateTime createdTimeEnd
    );
    
    /**
     * 根据ID查询入库单详情
     */
    StockIn getStockInById(Long id);
    
    /**
     * 创建入库单
     */
    StockIn createStockIn(StockInDTO dto);
    
    /**
     * 确认入库
     */
    void confirmStockIn(Long id);
    
    /**
     * 取消入库单
     */
    void cancelStockIn(Long id);
}
