package com.lab.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.material.dto.MaterialDTO;
import com.lab.material.entity.Material;
import com.lab.material.entity.MaterialCategory;
import com.lab.material.entity.Supplier;
import com.lab.material.mapper.MaterialCategoryMapper;
import com.lab.material.mapper.MaterialMapper;
import com.lab.material.mapper.SupplierMapper;
import com.lab.material.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * 药品信息服务实现
 */
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {
    
    private final MaterialMapper materialMapper;
    private final MaterialCategoryMapper categoryMapper;
    private final SupplierMapper supplierMapper;
    
    @Override
    public Page<MaterialDTO> getMaterialPage(int page, int size, Integer materialType, String keyword, Long categoryId) {
        Page<Material> materialPage = new Page<>(page, size);
        
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        
        if (materialType != null) {
            wrapper.eq(Material::getMaterialType, materialType);
        }
        
        if (categoryId != null) {
            wrapper.eq(Material::getCategoryId, categoryId);
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(Material::getMaterialName, keyword)
                .or()
                .like(Material::getMaterialCode, keyword)
            );
        }
        
        wrapper.orderByDesc(Material::getCreatedTime);
        
        Page<Material> result = materialMapper.selectPage(materialPage, wrapper);
        
        // 转换为DTO
        Page<MaterialDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        
        return dtoPage;
    }
    
    @Override
    public MaterialDTO getMaterialById(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new BusinessException("药品不存在");
        }
        return convertToDTO(material);
    }
    
    @Override
    public Material createMaterial(Material material) {
        // 检查药品编码是否已存在
        Long count = materialMapper.selectCount(
            new LambdaQueryWrapper<Material>()
                .eq(Material::getMaterialCode, material.getMaterialCode())
        );
        if (count > 0) {
            throw new BusinessException("药品编码已存在");
        }
        
        // 验证分类是否存在
        MaterialCategory category = categoryMapper.selectById(material.getCategoryId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 验证供应商是否存在（如果提供了供应商ID）
        if (material.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(material.getSupplierId());
            if (supplier == null) {
                throw new BusinessException("供应商不存在");
            }
        }
        
        // 验证危化品必填字段
        if (material.getMaterialType() == 3) {
            if (!StringUtils.hasText(material.getCasNumber())) {
                throw new BusinessException("危化品必须填写CAS号");
            }
            if (!StringUtils.hasText(material.getDangerCategory())) {
                throw new BusinessException("危化品必须填写危险类别");
            }
            if (material.getIsControlled() == null) {
                throw new BusinessException("危化品必须标识管控类型");
            }
        }
        
        materialMapper.insert(material);
        return material;
    }
    
    @Override
    public Material updateMaterial(Long id, Material material) {
        Material existing = materialMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("药品不存在");
        }
        
        // 检查药品编码是否与其他药品重复
        if (!existing.getMaterialCode().equals(material.getMaterialCode())) {
            Long count = materialMapper.selectCount(
                new LambdaQueryWrapper<Material>()
                    .eq(Material::getMaterialCode, material.getMaterialCode())
                    .ne(Material::getId, id)
            );
            if (count > 0) {
                throw new BusinessException("药品编码已存在");
            }
        }
        
        // 验证分类是否存在
        MaterialCategory category = categoryMapper.selectById(material.getCategoryId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 验证供应商是否存在（如果提供了供应商ID）
        if (material.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(material.getSupplierId());
            if (supplier == null) {
                throw new BusinessException("供应商不存在");
            }
        }
        
        // 验证危化品必填字段
        if (material.getMaterialType() == 3) {
            if (!StringUtils.hasText(material.getCasNumber())) {
                throw new BusinessException("危化品必须填写CAS号");
            }
            if (!StringUtils.hasText(material.getDangerCategory())) {
                throw new BusinessException("危化品必须填写危险类别");
            }
            if (material.getIsControlled() == null) {
                throw new BusinessException("危化品必须标识管控类型");
            }
        }
        
        material.setId(id);
        materialMapper.updateById(material);
        return material;
    }
    
    @Override
    public void deleteMaterial(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new BusinessException("药品不存在");
        }
        
        materialMapper.deleteById(id);
    }
    
    @Override
    public java.util.List<MaterialDTO> getHazardousMaterials() {
        // 查询药品类型为3(危化品)或管控类型>0的药品
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
            .eq(Material::getMaterialType, 3)
            .or()
            .gt(Material::getIsControlled, 0)
        );
        wrapper.eq(Material::getStatus, 1); // 只查询启用状态的药品
        wrapper.orderByDesc(Material::getCreatedTime);
        
        java.util.List<Material> materials = materialMapper.selectList(wrapper);
        return materials.stream().map(this::convertToDTO).toList();
    }
    
    private MaterialDTO convertToDTO(Material material) {
        MaterialDTO dto = new MaterialDTO();
        BeanUtils.copyProperties(material, dto);
        
        // 填充分类名称
        if (material.getCategoryId() != null) {
            MaterialCategory category = categoryMapper.selectById(material.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getCategoryName());
            }
        }
        
        // 填充供应商名称
        if (material.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(material.getSupplierId());
            if (supplier != null) {
                dto.setSupplierName(supplier.getSupplierName());
            }
        }
        
        return dto;
    }
}
