package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.annotation.AuditLog;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.dto.StockCheckDTO;
import com.lab.inventory.entity.StockCheck;
import com.lab.inventory.entity.StockCheckDetail;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.StockCheckDetailMapper;
import com.lab.inventory.mapper.StockCheckMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 库存盘点服务实现
 */
@Service
@RequiredArgsConstructor
public class StockCheckServiceImpl implements com.lab.inventory.service.StockCheckService {
    
    private final StockCheckMapper stockCheckMapper;
    private final StockCheckDetailMapper stockCheckDetailMapper;
    private final StockInventoryMapper stockInventoryMapper;
    
    @Override
    public Page<StockCheck> listStockCheck(int page, int size, String keyword, Long warehouseId, Integer status) {
        Page<StockCheck> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<StockCheck> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(StockCheck::getCheckNo, keyword.trim());
        }
        if (warehouseId != null) {
            wrapper.eq(StockCheck::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(StockCheck::getStatus, status);
        }
        
        wrapper.orderByDesc(StockCheck::getCreatedTime);
        return stockCheckMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public StockCheck getStockCheckById(Long id) {
        StockCheck stockCheck = stockCheckMapper.selectById(id);
        if (stockCheck == null) {
            throw new BusinessException("盘点单不存在");
        }

        LambdaQueryWrapper<StockCheckDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(StockCheckDetail::getCheckId, id);
        detailWrapper.orderByAsc(StockCheckDetail::getId);
        List<StockCheckDetail> details = stockCheckDetailMapper.selectList(detailWrapper);
        stockCheck.setItems(details);

        return stockCheck;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CREATE", businessType = "STOCK_CHECK", description = "创建盘点单")
    public StockCheck createStockCheck(StockCheckDTO dto) {
        // 生成盘点单号
        String checkNo = generateCheckNo();
        LocalDateTime now = LocalDateTime.now();
        
        // 创建盘点单
        StockCheck stockCheck = new StockCheck();
        BeanUtils.copyProperties(dto, stockCheck);
        stockCheck.setCheckNo(checkNo);
        stockCheck.setStatus(1); // 盘点中
        stockCheck.setCreatedBy(dto.getCheckerId());
        stockCheck.setCreatedTime(now);
        stockCheck.setUpdatedTime(now);
        
        stockCheckMapper.insert(stockCheck);
        
        return stockCheck;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "SUBMIT", businessType = "STOCK_CHECK", description = "提交盘点明细")
    public void submitCheckDetails(Long id, StockCheckDTO dto) {
        StockCheck stockCheck = getStockCheckById(id);
        
        if (stockCheck.getStatus() != 1) {
            throw new BusinessException("盘点单状态不允许提交明细");
        }
        
        // 删除旧的盘点明细
        LambdaQueryWrapper<StockCheckDetail> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(StockCheckDetail::getCheckId, id);
        stockCheckDetailMapper.delete(deleteWrapper);
        
        // 创建新的盘点明细
        LocalDateTime now = LocalDateTime.now();
        for (StockCheckDTO.StockCheckDetailDTO itemDto : dto.getItems()) {
            StockCheckDetail detail = new StockCheckDetail();
            BeanUtils.copyProperties(itemDto, detail);
            detail.setCheckId(id);
            detail.setCreatedTime(now);
            
            // 计算差异数量
            BigDecimal diffQty = itemDto.getActualQuantity().subtract(itemDto.getBookQuantity());
            detail.setDiffQuantity(diffQty);
            
            stockCheckDetailMapper.insert(detail);
        }

        stockCheck.setUpdatedTime(now);
        stockCheckMapper.updateById(stockCheck);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "COMPLETE", businessType = "STOCK_CHECK", description = "完成盘点")
    public void completeStockCheck(Long id) {
        StockCheck stockCheck = getStockCheckById(id);
        
        if (stockCheck.getStatus() != 1) {
            throw new BusinessException("盘点单状态不允许完成");
        }
        
        // 查询盘点明细
        LambdaQueryWrapper<StockCheckDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockCheckDetail::getCheckId, id);
        List<StockCheckDetail> details = stockCheckDetailMapper.selectList(wrapper);
        
        // 调整库存
        for (StockCheckDetail detail : details) {
            if (detail.getDiffQuantity().compareTo(BigDecimal.ZERO) != 0) {
                adjustInventory(stockCheck, detail);
            }
        }
        
        // 更新盘点单状态
        stockCheck.setStatus(2); // 已完成
        stockCheck.setUpdatedTime(LocalDateTime.now());
        stockCheckMapper.updateById(stockCheck);
    }
    
    /**
     * 调整库存
     */
    private void adjustInventory(StockCheck stockCheck, StockCheckDetail detail) {
        // 查询库存
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInventory::getMaterialId, detail.getMaterialId());
        wrapper.eq(StockInventory::getWarehouseId, stockCheck.getWarehouseId());
        wrapper.eq(StockInventory::getBatchNumber, detail.getBatchNumber());
        
        StockInventory inventory = stockInventoryMapper.selectOne(wrapper);
        
        if (inventory == null) {
            throw new BusinessException("库存不存在");
        }
        
        // 调整库存数量
        inventory.setQuantity(detail.getActualQuantity());
        inventory.setAvailableQuantity(inventory.getAvailableQuantity().add(detail.getDiffQuantity()));
        inventory.setLastCheckDate(stockCheck.getCheckDate());
        
        if (inventory.getUnitPrice() != null) {
            inventory.setTotalAmount(inventory.getQuantity().multiply(inventory.getUnitPrice()));
        }
        
        stockInventoryMapper.updateById(inventory);
    }
    
    /**
     * 生成盘点单号
     */
    private String generateCheckNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CHK" + date;
        
        // 查询当天最大单号
        LambdaQueryWrapper<StockCheck> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(StockCheck::getCheckNo, prefix);
        wrapper.orderByDesc(StockCheck::getCheckNo);
        wrapper.last("LIMIT 1");
        
        StockCheck lastCheck = stockCheckMapper.selectOne(wrapper);
        
        int sequence = 1;
        if (lastCheck != null) {
            String lastNo = lastCheck.getCheckNo();
            sequence = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        
        return prefix + String.format("%04d", sequence);
    }
}
