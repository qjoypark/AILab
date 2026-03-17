package com.lab.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.inventory.dto.WarehouseDTO;
import com.lab.inventory.entity.Warehouse;

/**
 * 仓库服务接口
 */
public interface WarehouseService {
    
    /**
     * 分页查询仓库列表
     */
    Page<Warehouse> listWarehouses(int page, int size, Integer warehouseType);
    
    /**
     * 根据ID查询仓库
     */
    Warehouse getWarehouseById(Long id);
    
    /**
     * 创建仓库
     */
    Warehouse createWarehouse(WarehouseDTO dto);
    
    /**
     * 更新仓库
     */
    Warehouse updateWarehouse(Long id, WarehouseDTO dto);
    
    /**
     * 删除仓库
     */
    void deleteWarehouse(Long id);
}
