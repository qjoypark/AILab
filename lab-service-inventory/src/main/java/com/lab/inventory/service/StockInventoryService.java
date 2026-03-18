package com.lab.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.inventory.entity.StockInventory;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存服务接口
 */
public interface StockInventoryService {
    
    /**
     * 分页查询库存列表
     */
    Page<StockInventory> listStock(
            int page,
            int size,
            Long materialId,
            String keyword,
            Long warehouseId,
            Boolean lowStock
    );
    
    /**
     * 查询指定药品的库存明细
     */
    List<StockInventory> getStockDetailByMaterialId(Long materialId);
    
    /**
     * 查询指定药品在指定仓库的库存
     */
    StockInventory getStockByMaterialAndWarehouse(Long materialId, Long warehouseId, String batchNumber);
    
    /**
     * 更新库存数量（入库）
     */
    void increaseStock(Long materialId, Long warehouseId, String batchNumber, BigDecimal quantity);
    
    /**
     * 减少库存数量（出库）
     */
    void decreaseStock(Long materialId, Long warehouseId, String batchNumber, BigDecimal quantity);
    
    /**
     * 调整库存数量（盘点）
     */
    void adjustStock(Long materialId, Long warehouseId, String batchNumber, BigDecimal quantity);
}
