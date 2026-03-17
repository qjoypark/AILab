package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.dto.StockAlertConfigDTO;
import com.lab.inventory.entity.StockAlertConfig;
import com.lab.inventory.mapper.StockAlertConfigMapper;
import com.lab.inventory.service.StockAlertConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 库存预警配置服务实现
 */
@Service
@RequiredArgsConstructor
public class StockAlertConfigServiceImpl implements StockAlertConfigService {
    
    private final StockAlertConfigMapper alertConfigMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAlertConfig(StockAlertConfigDTO dto) {
        // 检查是否已存在相同配置
        LambdaQueryWrapper<StockAlertConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockAlertConfig::getMaterialId, dto.getMaterialId())
               .eq(StockAlertConfig::getAlertType, dto.getAlertType());
        
        if (alertConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该药品的此类型预警配置已存在");
        }
        
        StockAlertConfig config = new StockAlertConfig();
        BeanUtils.copyProperties(dto, config);
        config.setStatus(1);
        
        alertConfigMapper.insert(config);
        return config.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlertConfig(Long id, StockAlertConfigDTO dto) {
        StockAlertConfig config = alertConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("预警配置不存在");
        }
        
        BeanUtils.copyProperties(dto, config);
        config.setId(id);
        alertConfigMapper.updateById(config);
    }
    
    @Override
    public Page<StockAlertConfigDTO> listAlertConfigs(int page, int size, Long materialId, Integer alertType) {
        Page<StockAlertConfig> configPage = new Page<>(page, size);
        LambdaQueryWrapper<StockAlertConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (materialId != null) {
            wrapper.eq(StockAlertConfig::getMaterialId, materialId);
        }
        if (alertType != null) {
            wrapper.eq(StockAlertConfig::getAlertType, alertType);
        }
        
        wrapper.orderByDesc(StockAlertConfig::getCreatedTime);
        alertConfigMapper.selectPage(configPage, wrapper);
        
        Page<StockAlertConfigDTO> dtoPage = new Page<>(page, size);
        dtoPage.setTotal(configPage.getTotal());
        dtoPage.setRecords(configPage.getRecords().stream().map(config -> {
            StockAlertConfigDTO dto = new StockAlertConfigDTO();
            BeanUtils.copyProperties(config, dto);
            return dto;
        }).collect(Collectors.toList()));
        
        return dtoPage;
    }
    
    @Override
    public StockAlertConfigDTO getAlertConfig(Long id) {
        StockAlertConfig config = alertConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("预警配置不存在");
        }
        
        StockAlertConfigDTO dto = new StockAlertConfigDTO();
        BeanUtils.copyProperties(config, dto);
        return dto;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlertConfig(Long id) {
        StockAlertConfig config = alertConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("预警配置不存在");
        }
        
        alertConfigMapper.deleteById(id);
    }
}
