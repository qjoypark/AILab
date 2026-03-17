package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.annotation.AuditLog;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.dto.StorageLocationDTO;
import com.lab.inventory.entity.StorageLocation;
import com.lab.inventory.mapper.StorageLocationMapper;
import com.lab.inventory.service.StorageLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 存储位置服务实现
 */
@Service
@RequiredArgsConstructor
public class StorageLocationServiceImpl implements StorageLocationService {
    
    private final StorageLocationMapper storageLocationMapper;
    
    @Override
    public Page<StorageLocation> listStorageLocations(int page, int size, Long warehouseId) {
        Page<StorageLocation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<StorageLocation> wrapper = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            wrapper.eq(StorageLocation::getWarehouseId, warehouseId);
        }
        wrapper.orderByDesc(StorageLocation::getCreatedTime);
        return storageLocationMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public List<StorageLocation> listByWarehouseId(Long warehouseId) {
        LambdaQueryWrapper<StorageLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StorageLocation::getWarehouseId, warehouseId);
        wrapper.eq(StorageLocation::getStatus, 1);
        return storageLocationMapper.selectList(wrapper);
    }
    
    @Override
    public StorageLocation getStorageLocationById(Long id) {
        StorageLocation location = storageLocationMapper.selectById(id);
        if (location == null) {
            throw new BusinessException("存储位置不存在");
        }
        return location;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CREATE", businessType = "STORAGE_LOCATION", description = "创建存储位置")
    public StorageLocation createStorageLocation(StorageLocationDTO dto) {
        // 检查位置编码在同一仓库中是否已存在
        LambdaQueryWrapper<StorageLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StorageLocation::getWarehouseId, dto.getWarehouseId());
        wrapper.eq(StorageLocation::getLocationCode, dto.getLocationCode());
        if (storageLocationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该仓库中位置编码已存在");
        }
        
        StorageLocation location = new StorageLocation();
        BeanUtils.copyProperties(dto, location);
        storageLocationMapper.insert(location);
        return location;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "UPDATE", businessType = "STORAGE_LOCATION", description = "更新存储位置")
    public StorageLocation updateStorageLocation(Long id, StorageLocationDTO dto) {
        StorageLocation location = getStorageLocationById(id);
        
        // 检查位置编码在同一仓库中是否已被其他位置使用
        if (!location.getLocationCode().equals(dto.getLocationCode()) || 
            !location.getWarehouseId().equals(dto.getWarehouseId())) {
            LambdaQueryWrapper<StorageLocation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StorageLocation::getWarehouseId, dto.getWarehouseId());
            wrapper.eq(StorageLocation::getLocationCode, dto.getLocationCode());
            wrapper.ne(StorageLocation::getId, id);
            if (storageLocationMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("该仓库中位置编码已存在");
            }
        }
        
        BeanUtils.copyProperties(dto, location);
        storageLocationMapper.updateById(location);
        return location;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "DELETE", businessType = "STORAGE_LOCATION", description = "删除存储位置")
    public void deleteStorageLocation(Long id) {
        StorageLocation location = getStorageLocationById(id);
        storageLocationMapper.deleteById(id);
    }
}
