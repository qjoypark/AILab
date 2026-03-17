package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.StockInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存服务实现
 */
@Service
@RequiredArgsConstructor
public class StockInventoryServiceImpl implements StockInventoryService {
    
    private final StockInventoryMapper stockInventoryMapper;
    
    @Override
    public Page<StockInventory> listStock(int page, int size, Long materialId, Long warehouseId, Boolean lowStock) {
        Page<StockInventory> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        
        if (materialId != null) {
            wrapper.eq(StockInventory::getMaterialId, materialId);
        }
        if (warehouseId != null) {
            wrapper.eq(StockInventory::getWarehouseId, warehouseId);
        }
        // TODO: 实现低库存筛选逻辑（需要关联material表获取安全库存）
        
        wrapper.orderByDesc(StockInventory::getUpdatedTime);
        return stockInventoryMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public List<StockInventory> getStockDetailByMaterialId(Long materialId) {
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInventory::getMaterialId, materialId);
        wrapper.orderByAsc(StockInventory::getProductionDate);
        return stockInventoryMapper.selectList(wrapper);
    }
    
    @Override
    public StockInventory getStockByMaterialAndWarehouse(Long materialId, Long warehouseId, String batchNumber) {
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInventory::getMaterialId, materialId);
        wrapper.eq(StockInventory::getWarehouseId, warehouseId);
        wrapper.eq(StockInventory::getBatchNumber, batchNumber);
        return stockInventoryMapper.selectOne(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long materialId, Long warehouseId, String batchNumber, BigDecimal quantity) {
        StockInventory stock = getStockByMaterialAndWarehouse(materialId, warehouseId, batchNumber);
        if (stock != null) {
            // 更新现有库存
            stock.setQuantity(stock.getQuantity().add(quantity));
            stock.setAvailableQuantity(stock.getAvailableQuantity().add(quantity));
            if (stock.getUnitPrice() != null) {
                stock.setTotalAmount(stock.getQuantity().multiply(stock.getUnitPrice()));
            }
            stockInventoryMapper.updateById(stock);
        }
        // 如果库存不存在，应该在入库确认时创建
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long materialId, Long warehouseId, String batchNumber, BigDecimal quantity) {
        StockInventory stock = getStockByMaterialAndWarehouse(materialId, warehouseId, batchNumber);
        if (stock == null) {
            throw new BusinessException("库存不存在");
        }
        
        if (stock.getAvailableQuantity().compareTo(quantity) < 0) {
            throw new BusinessException("库存不足");
        }
        
        stock.setQuantity(stock.getQuantity().subtract(quantity));
        stock.setAvailableQuantity(stock.getAvailableQuantity().subtract(quantity));
        if (stock.getUnitPrice() != null) {
            stock.setTotalAmount(stock.getQuantity().multiply(stock.getUnitPrice()));
        }
        stockInventoryMapper.updateById(stock);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long materialId, Long warehouseId, String batchNumber, BigDecimal quantity) {
        StockInventory stock = getStockByMaterialAndWarehouse(materialId, warehouseId, batchNumber);
        if (stock == null) {
            throw new BusinessException("库存不存在");
        }
        
        BigDecimal diff = quantity.subtract(stock.getQuantity());
        stock.setQuantity(quantity);
        stock.setAvailableQuantity(stock.getAvailableQuantity().add(diff));
        if (stock.getUnitPrice() != null) {
            stock.setTotalAmount(stock.getQuantity().multiply(stock.getUnitPrice()));
        }
        stockInventoryMapper.updateById(stock);
    }
}
