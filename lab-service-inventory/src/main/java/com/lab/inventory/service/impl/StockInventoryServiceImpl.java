package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.entity.StorageLocation;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.entity.Warehouse;
import com.lab.inventory.mapper.StorageLocationMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.mapper.WarehouseMapper;
import com.lab.inventory.service.StockInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 库存服务实现
 */
@Service
@RequiredArgsConstructor
public class StockInventoryServiceImpl implements StockInventoryService {
    
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    private final StockInventoryMapper stockInventoryMapper;
    private final MaterialClient materialClient;
    private final WarehouseMapper warehouseMapper;
    private final StorageLocationMapper storageLocationMapper;
    
    @Override
    public Page<StockInventory> listStock(
            int page,
            int size,
            Long materialId,
            String keyword,
            Long warehouseId,
            Boolean lowStock
    ) {
        Page<StockInventory> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        
        if (materialId != null) {
            wrapper.eq(StockInventory::getMaterialId, materialId);
        }

        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            List<Long> materialIds = resolveMaterialIdsByKeyword(normalizedKeyword);
            List<Long> warehouseIds = searchWarehouseIdsByKeyword(normalizedKeyword);
            List<Long> locationIds = searchStorageLocationIdsByKeyword(normalizedKeyword);

            wrapper.and(condition -> {
                boolean hasAppended = false;

                if (!materialIds.isEmpty()) {
                    condition.in(StockInventory::getMaterialId, materialIds);
                    hasAppended = true;
                }
                if (StringUtils.hasText(normalizedKeyword)) {
                    if (hasAppended) {
                        condition.or();
                    }
                    condition.like(StockInventory::getBatchNumber, normalizedKeyword);
                    hasAppended = true;
                }
                if (!warehouseIds.isEmpty()) {
                    if (hasAppended) {
                        condition.or();
                    }
                    condition.in(StockInventory::getWarehouseId, warehouseIds);
                    hasAppended = true;
                }
                if (!locationIds.isEmpty()) {
                    if (hasAppended) {
                        condition.or();
                    }
                    condition.in(StockInventory::getStorageLocationId, locationIds);
                }
            });
        }

        if (warehouseId != null) {
            wrapper.eq(StockInventory::getWarehouseId, warehouseId);
        }

        wrapper.orderByDesc(StockInventory::getUpdatedTime);
        if (!Boolean.TRUE.equals(lowStock)) {
            return stockInventoryMapper.selectPage(pageParam, wrapper);
        }

        List<StockInventory> candidateList = stockInventoryMapper.selectList(wrapper);
        if (candidateList.isEmpty()) {
            pageParam.setTotal(0);
            pageParam.setRecords(List.of());
            return pageParam;
        }

        Map<Long, Integer> safetyStockMap = new HashMap<>();
        List<StockInventory> filteredList = candidateList.stream()
                .filter(stock -> isLowStock(stock, safetyStockMap))
                .toList();

        long total = filteredList.size();
        long current = Math.max(page, 1);
        long pageSize = Math.max(size, 1);
        int fromIndex = (int) ((current - 1) * pageSize);
        int toIndex = (int) Math.min(fromIndex + pageSize, total);
        List<StockInventory> pageRecords = new ArrayList<>();
        if (fromIndex < toIndex) {
            pageRecords = filteredList.subList(fromIndex, toIndex);
        }

        Page<StockInventory> result = new Page<>(current, pageSize, total);
        result.setRecords(pageRecords);
        return result;
    }

    private boolean isLowStock(StockInventory stock, Map<Long, Integer> safetyStockMap) {
        Integer threshold = safetyStockMap.computeIfAbsent(
                stock.getMaterialId(),
                this::resolveSafetyStock
        );
        BigDecimal thresholdValue = BigDecimal.valueOf(threshold);
        BigDecimal availableQuantity = stock.getAvailableQuantity() != null ? stock.getAvailableQuantity() : BigDecimal.ZERO;
        return availableQuantity.compareTo(thresholdValue) <= 0;
    }

    private Integer resolveSafetyStock(Long materialId) {
        try {
            var materialInfo = materialClient.getMaterialInfo(materialId);
            if (materialInfo != null && materialInfo.getSafetyStock() != null && materialInfo.getSafetyStock() > 0) {
                return materialInfo.getSafetyStock();
            }
        } catch (Exception ignored) {
            // fall through to default threshold
        }
        return DEFAULT_LOW_STOCK_THRESHOLD;
    }

    private Long parseMaterialId(String keyword) {
        try {
            return Long.parseLong(keyword);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<Long> resolveMaterialIdsByKeyword(String keyword) {
        Set<Long> materialIdSet = new LinkedHashSet<>();
        Long parsedId = parseMaterialId(keyword);
        if (parsedId != null) {
            materialIdSet.add(parsedId);
        }

        List<Long> searchedMaterialIds = materialClient.searchMaterialIdsByKeyword(keyword);
        if (searchedMaterialIds != null && !searchedMaterialIds.isEmpty()) {
            materialIdSet.addAll(searchedMaterialIds);
        }
        return new ArrayList<>(materialIdSet);
    }

    private List<Long> searchWarehouseIdsByKeyword(String keyword) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Warehouse::getWarehouseName, keyword)
                .or()
                .like(Warehouse::getWarehouseCode, keyword)
                .or()
                .like(Warehouse::getLocation, keyword);
        List<Warehouse> warehouses = warehouseMapper.selectList(wrapper);
        if (warehouses == null || warehouses.isEmpty()) {
            return List.of();
        }
        return warehouses.stream().map(Warehouse::getId).toList();
    }

    private List<Long> searchStorageLocationIdsByKeyword(String keyword) {
        LambdaQueryWrapper<StorageLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StorageLocation::getLocationName, keyword)
                .or()
                .like(StorageLocation::getLocationCode, keyword)
                .or()
                .like(StorageLocation::getShelfNumber, keyword)
                .or()
                .like(StorageLocation::getLayerNumber, keyword);
        List<StorageLocation> locations = storageLocationMapper.selectList(wrapper);
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }
        return locations.stream().map(StorageLocation::getId).toList();
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
