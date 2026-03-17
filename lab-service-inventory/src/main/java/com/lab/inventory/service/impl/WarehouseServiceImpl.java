package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.annotation.AuditLog;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.dto.WarehouseDTO;
import com.lab.inventory.entity.Warehouse;
import com.lab.inventory.mapper.WarehouseMapper;
import com.lab.inventory.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 仓库服务实现
 */
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {
    
    private final WarehouseMapper warehouseMapper;
    
    @Override
    public Page<Warehouse> listWarehouses(int page, int size, Integer warehouseType) {
        Page<Warehouse> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (warehouseType != null) {
            wrapper.eq(Warehouse::getWarehouseType, warehouseType);
        }
        wrapper.orderByDesc(Warehouse::getCreatedTime);
        return warehouseMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public Warehouse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BusinessException("仓库不存在");
        }
        return warehouse;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CREATE", businessType = "WAREHOUSE", description = "创建仓库")
    public Warehouse createWarehouse(WarehouseDTO dto) {
        // 检查仓库编码是否已存在
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Warehouse::getWarehouseCode, dto.getWarehouseCode());
        if (warehouseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("仓库编码已存在");
        }
        
        Warehouse warehouse = new Warehouse();
        BeanUtils.copyProperties(dto, warehouse);
        warehouseMapper.insert(warehouse);
        return warehouse;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "UPDATE", businessType = "WAREHOUSE", description = "更新仓库")
    public Warehouse updateWarehouse(Long id, WarehouseDTO dto) {
        Warehouse warehouse = getWarehouseById(id);
        
        // 检查仓库编码是否已被其他仓库使用
        if (!warehouse.getWarehouseCode().equals(dto.getWarehouseCode())) {
            LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Warehouse::getWarehouseCode, dto.getWarehouseCode());
            wrapper.ne(Warehouse::getId, id);
            if (warehouseMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("仓库编码已存在");
            }
        }
        
        BeanUtils.copyProperties(dto, warehouse);
        warehouseMapper.updateById(warehouse);
        return warehouse;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "DELETE", businessType = "WAREHOUSE", description = "删除仓库")
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = getWarehouseById(id);
        warehouseMapper.deleteById(id);
    }
}
