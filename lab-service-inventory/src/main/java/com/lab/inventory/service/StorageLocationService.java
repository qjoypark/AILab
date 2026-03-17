package com.lab.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.inventory.dto.StorageLocationDTO;
import com.lab.inventory.entity.StorageLocation;

import java.util.List;

/**
 * 存储位置服务接口
 */
public interface StorageLocationService {
    
    /**
     * 分页查询存储位置列表
     */
    Page<StorageLocation> listStorageLocations(int page, int size, Long warehouseId);
    
    /**
     * 根据仓库ID查询所有存储位置
     */
    List<StorageLocation> listByWarehouseId(Long warehouseId);
    
    /**
     * 根据ID查询存储位置
     */
    StorageLocation getStorageLocationById(Long id);
    
    /**
     * 创建存储位置
     */
    StorageLocation createStorageLocation(StorageLocationDTO dto);
    
    /**
     * 更新存储位置
     */
    StorageLocation updateStorageLocation(Long id, StorageLocationDTO dto);
    
    /**
     * 删除存储位置
     */
    void deleteStorageLocation(Long id);
}
