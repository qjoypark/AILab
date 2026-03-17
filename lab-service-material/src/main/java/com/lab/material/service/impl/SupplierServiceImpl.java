package com.lab.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.material.dto.SupplierDTO;
import com.lab.material.entity.Supplier;
import com.lab.material.mapper.SupplierMapper;
import com.lab.material.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * 供应商服务实现
 */
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    
    private final SupplierMapper supplierMapper;
    
    @Override
    public Page<SupplierDTO> getSupplierPage(int page, int size, String keyword) {
        Page<Supplier> supplierPage = new Page<>(page, size);
        
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(Supplier::getSupplierName, keyword)
                .or()
                .like(Supplier::getSupplierCode, keyword)
            );
        }
        
        wrapper.orderByDesc(Supplier::getCreatedTime);
        
        Page<Supplier> result = supplierMapper.selectPage(supplierPage, wrapper);
        
        // 转换为DTO
        Page<SupplierDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        
        return dtoPage;
    }
    
    @Override
    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        return convertToDTO(supplier);
    }
    
    @Override
    public Supplier createSupplier(Supplier supplier) {
        // 检查供应商编码是否已存在
        Long count = supplierMapper.selectCount(
            new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getSupplierCode, supplier.getSupplierCode())
        );
        if (count > 0) {
            throw new BusinessException("供应商编码已存在");
        }
        
        supplierMapper.insert(supplier);
        return supplier;
    }
    
    @Override
    public Supplier updateSupplier(Long id, Supplier supplier) {
        Supplier existing = supplierMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("供应商不存在");
        }
        
        // 检查供应商编码是否与其他供应商重复
        if (!existing.getSupplierCode().equals(supplier.getSupplierCode())) {
            Long count = supplierMapper.selectCount(
                new LambdaQueryWrapper<Supplier>()
                    .eq(Supplier::getSupplierCode, supplier.getSupplierCode())
                    .ne(Supplier::getId, id)
            );
            if (count > 0) {
                throw new BusinessException("供应商编码已存在");
            }
        }
        
        supplier.setId(id);
        supplierMapper.updateById(supplier);
        return supplier;
    }
    
    @Override
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        
        supplierMapper.deleteById(id);
    }
    
    private SupplierDTO convertToDTO(Supplier supplier) {
        SupplierDTO dto = new SupplierDTO();
        BeanUtils.copyProperties(supplier, dto);
        return dto;
    }
}
