package com.lab.material.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.material.dto.SupplierDTO;
import com.lab.material.entity.Supplier;

/**
 * 供应商服务接口
 */
public interface SupplierService {
    
    /**
     * 分页查询供应商列表
     */
    Page<SupplierDTO> getSupplierPage(int page, int size, String keyword);
    
    /**
     * 根据ID查询供应商
     */
    SupplierDTO getSupplierById(Long id);
    
    /**
     * 创建供应商
     */
    Supplier createSupplier(Supplier supplier);
    
    /**
     * 更新供应商
     */
    Supplier updateSupplier(Long id, Supplier supplier);
    
    /**
     * 删除供应商
     */
    void deleteSupplier(Long id);
}
