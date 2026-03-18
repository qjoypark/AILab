package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.annotation.AuditLog;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.dto.StockInDTO;
import com.lab.inventory.entity.StockIn;
import com.lab.inventory.entity.StockInDetail;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.StockInDetailMapper;
import com.lab.inventory.mapper.StockInMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 入库服务实现
 */
@Service
@RequiredArgsConstructor
public class StockInServiceImpl implements com.lab.inventory.service.StockInService {
    
    private final StockInMapper stockInMapper;
    private final StockInDetailMapper stockInDetailMapper;
    private final StockInventoryMapper stockInventoryMapper;
    
    @Override
    public Page<StockIn> listStockIn(
            int page,
            int size,
            Long warehouseId,
            Integer status,
            LocalDateTime createdTimeStart,
            LocalDateTime createdTimeEnd
    ) {
        Page<StockIn> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();
        
        if (warehouseId != null) {
            wrapper.eq(StockIn::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(StockIn::getStatus, status);
        }
        if (createdTimeStart != null) {
            wrapper.ge(StockIn::getCreatedTime, createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le(StockIn::getCreatedTime, createdTimeEnd);
        }
        
        wrapper.orderByDesc(StockIn::getCreatedTime);
        return stockInMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public StockIn getStockInById(Long id) {
        StockIn stockIn = stockInMapper.selectById(id);
        if (stockIn == null) {
            throw new BusinessException("入库单不存在");
        }

        LambdaQueryWrapper<StockInDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(StockInDetail::getInOrderId, id);
        detailWrapper.orderByAsc(StockInDetail::getId);
        List<StockInDetail> details = stockInDetailMapper.selectList(detailWrapper);
        stockIn.setItems(details);

        return stockIn;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CREATE", businessType = "STOCK_IN", description = "创建入库单")
    public StockIn createStockIn(StockInDTO dto) {
        // 生成入库单号
        String inOrderNo = generateInOrderNo();
        LocalDateTime now = LocalDateTime.now();
        
        // 创建入库单
        StockIn stockIn = new StockIn();
        BeanUtils.copyProperties(dto, stockIn);
        stockIn.setInOrderNo(inOrderNo);
        stockIn.setStatus(1); // 待入库
        stockIn.setCreatedBy(dto.getOperatorId());
        stockIn.setCreatedTime(now);
        stockIn.setUpdatedBy(dto.getOperatorId());
        stockIn.setUpdatedTime(now);
        
        // 计算总金额
        BigDecimal totalAmount = dto.getItems().stream()
            .map(item -> {
                BigDecimal qty = item.getQuantity();
                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                return qty.multiply(price);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stockIn.setTotalAmount(totalAmount);
        
        stockInMapper.insert(stockIn);
        
        // 创建入库明细
        for (StockInDTO.StockInDetailDTO itemDto : dto.getItems()) {
            StockInDetail detail = new StockInDetail();
            BeanUtils.copyProperties(itemDto, detail);
            detail.setInOrderId(stockIn.getId());
            detail.setCreatedTime(now);
            
            if (itemDto.getUnitPrice() != null) {
                detail.setTotalAmount(itemDto.getQuantity().multiply(itemDto.getUnitPrice()));
            }
            
            stockInDetailMapper.insert(detail);
        }
        
        return stockIn;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CONFIRM", businessType = "STOCK_IN", description = "确认入库")
    public void confirmStockIn(Long id) {
        StockIn stockIn = getStockInById(id);
        
        if (stockIn.getStatus() != 1) {
            throw new BusinessException("入库单状态不允许确认");
        }
        
        // 查询入库明细
        LambdaQueryWrapper<StockInDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInDetail::getInOrderId, id);
        List<StockInDetail> details = stockInDetailMapper.selectList(wrapper);
        
        // 更新库存
        for (StockInDetail detail : details) {
            updateInventory(stockIn, detail);
        }
        
        // 更新入库单状态
        stockIn.setStatus(2); // 已入库
        stockIn.setUpdatedBy(stockIn.getOperatorId());
        stockIn.setUpdatedTime(LocalDateTime.now());
        stockInMapper.updateById(stockIn);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CANCEL", businessType = "STOCK_IN", description = "取消入库单")
    public void cancelStockIn(Long id) {
        StockIn stockIn = getStockInById(id);
        
        if (stockIn.getStatus() != 1) {
            throw new BusinessException("入库单状态不允许取消");
        }
        
        stockIn.setStatus(3); // 已取消
        stockIn.setUpdatedBy(stockIn.getOperatorId());
        stockIn.setUpdatedTime(LocalDateTime.now());
        stockInMapper.updateById(stockIn);
    }
    
    /**
     * 更新库存
     */
    private void updateInventory(StockIn stockIn, StockInDetail detail) {
        // 查询是否已存在该批次的库存
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInventory::getMaterialId, detail.getMaterialId());
        wrapper.eq(StockInventory::getWarehouseId, stockIn.getWarehouseId());
        wrapper.eq(StockInventory::getBatchNumber, detail.getBatchNumber());
        
        StockInventory inventory = stockInventoryMapper.selectOne(wrapper);
        
        if (inventory != null) {
            // 更新现有库存
            inventory.setQuantity(inventory.getQuantity().add(detail.getQuantity()));
            inventory.setAvailableQuantity(inventory.getAvailableQuantity().add(detail.getQuantity()));
            
            if (detail.getUnitPrice() != null) {
                inventory.setUnitPrice(detail.getUnitPrice());
                inventory.setTotalAmount(inventory.getQuantity().multiply(detail.getUnitPrice()));
            }
            
            stockInventoryMapper.updateById(inventory);
        } else {
            // 创建新库存记录
            inventory = new StockInventory();
            inventory.setMaterialId(detail.getMaterialId());
            inventory.setWarehouseId(stockIn.getWarehouseId());
            inventory.setStorageLocationId(detail.getStorageLocationId());
            inventory.setBatchNumber(detail.getBatchNumber());
            inventory.setQuantity(detail.getQuantity());
            inventory.setAvailableQuantity(detail.getQuantity());
            inventory.setLockedQuantity(BigDecimal.ZERO);
            inventory.setProductionDate(detail.getProductionDate());
            inventory.setExpireDate(detail.getExpireDate());
            inventory.setUnitPrice(detail.getUnitPrice());
            
            if (detail.getUnitPrice() != null) {
                inventory.setTotalAmount(detail.getQuantity().multiply(detail.getUnitPrice()));
            }
            
            stockInventoryMapper.insert(inventory);
        }
    }
    
    /**
     * 生成入库单号
     */
    private String generateInOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "IN" + date;
        
        // 查询当天最大单号
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(StockIn::getInOrderNo, prefix);
        wrapper.orderByDesc(StockIn::getInOrderNo);
        wrapper.last("LIMIT 1");
        
        StockIn lastOrder = stockInMapper.selectOne(wrapper);
        
        int sequence = 1;
        if (lastOrder != null) {
            String lastNo = lastOrder.getInOrderNo();
            sequence = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        
        return prefix + String.format("%04d", sequence);
    }
}
