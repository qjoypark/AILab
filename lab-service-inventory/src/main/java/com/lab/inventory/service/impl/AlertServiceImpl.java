package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.dto.AlertRecordDTO;
import com.lab.inventory.entity.AlertRecord;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.AlertRecordMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.AlertService;
import com.lab.inventory.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预警服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {
    
    private final AlertRecordMapper alertRecordMapper;
    private final StockInventoryMapper stockInventoryMapper;
    private final NotificationService notificationService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAlert(Integer alertType, Integer alertLevel, String businessType,
                           Long businessId, String title, String content) {
        AlertRecord alert = new AlertRecord();
        alert.setAlertType(alertType);
        alert.setAlertLevel(alertLevel);
        alert.setBusinessType(businessType);
        alert.setBusinessId(businessId);
        alert.setAlertTitle(title);
        alert.setAlertContent(content);
        alert.setAlertTime(LocalDateTime.now());
        alert.setStatus(1); // 未处理
        
        alertRecordMapper.insert(alert);
        log.info("创建预警记录: type={}, level={}, title={}", alertType, alertLevel, title);
        
        // 发送预警通知
        sendAlertNotification(alertType, alertLevel, title, content, businessType, businessId);
        
        return alert.getId();
    }
    
    /**
     * 根据预警类型和级别发送通知给相应的管理员
     */
    private void sendAlertNotification(Integer alertType, Integer alertLevel, String title, 
                                      String content, String businessType, Long businessId) {
        List<String> roleNames = new ArrayList<>();
        
        // 根据预警类型确定通知对象
        switch (alertType) {
            case 4: // 账实差异预警
                // 发送给中心管理员和安全管理员
                roleNames.add("CENTER_ADMIN");
                roleNames.add("SAFETY_ADMIN");
                break;
            case 1: // 低库存预警
            case 2: // 有效期预警
            case 3: // 异常消耗预警
                // 发送给中心管理员
                roleNames.add("CENTER_ADMIN");
                break;
            default:
                log.warn("未知的预警类型: {}", alertType);
                return;
        }
        
        // 发送通知
        notificationService.sendAlertNotification(roleNames, title, content, businessType, businessId);
    }
    
    @Override
    public Page<AlertRecordDTO> listAlerts(int page, int size, Integer alertType,
                                           Integer alertLevel, Integer status) {
        Page<AlertRecord> alertPage = new Page<>(page, size);
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (alertType != null) {
            wrapper.eq(AlertRecord::getAlertType, alertType);
        }
        if (alertLevel != null) {
            wrapper.eq(AlertRecord::getAlertLevel, alertLevel);
        }
        if (status != null) {
            wrapper.eq(AlertRecord::getStatus, status);
        }
        
        wrapper.orderByDesc(AlertRecord::getAlertTime);
        alertRecordMapper.selectPage(alertPage, wrapper);
        
        Page<AlertRecordDTO> dtoPage = new Page<>(page, size);
        dtoPage.setTotal(alertPage.getTotal());
        dtoPage.setRecords(alertPage.getRecords().stream().map(alert -> {
            AlertRecordDTO dto = new AlertRecordDTO();
            BeanUtils.copyProperties(alert, dto);
            return dto;
        }).collect(Collectors.toList()));
        
        return dtoPage;
    }
    
    @Override
    public AlertRecordDTO getAlert(Long id) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }
        
        AlertRecordDTO dto = new AlertRecordDTO();
        BeanUtils.copyProperties(alert, dto);
        return dto;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlert(Long id, Long handlerId, String handleRemark) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }
        
        if (alert.getStatus() != 1) {
            throw new BusinessException("预警已处理或已忽略");
        }
        
        alert.setStatus(2); // 已处理
        alert.setHandlerId(handlerId);
        alert.setHandleTime(LocalDateTime.now());
        alert.setHandleRemark(handleRemark);
        
        alertRecordMapper.updateById(alert);
        log.info("处理预警记录: id={}, handlerId={}", id, handlerId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ignoreAlert(Long id, Long handlerId) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException("预警记录不存在");
        }
        
        if (alert.getStatus() != 1) {
            throw new BusinessException("预警已处理或已忽略");
        }
        
        alert.setStatus(3); // 已忽略
        alert.setHandlerId(handlerId);
        alert.setHandleTime(LocalDateTime.now());
        
        alertRecordMapper.updateById(alert);
        log.info("忽略预警记录: id={}, handlerId={}", id, handlerId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkLowStockAlert() {
        log.info("开始检查低库存预警");
        
        // 查询所有库存记录
        List<StockInventory> inventories = stockInventoryMapper.selectList(null);
        
        int alertCount = 0;
        for (StockInventory inventory : inventories) {
            // 检查可用数量是否低于安全库存
            // 注意：安全库存存储在material表中，这里简化处理，假设安全库存为100
            BigDecimal safetyStock = new BigDecimal("100");
            
            if (inventory.getAvailableQuantity().compareTo(safetyStock) < 0) {
                // 检查是否已存在未处理的预警
                LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AlertRecord::getAlertType, 1)
                       .eq(AlertRecord::getBusinessType, "STOCK_INVENTORY")
                       .eq(AlertRecord::getBusinessId, inventory.getId())
                       .eq(AlertRecord::getStatus, 1);
                
                if (alertRecordMapper.selectCount(wrapper) == 0) {
                    String title = "低库存预警";
                    String content = String.format("药品ID: %d, 当前可用库存: %s, 低于安全库存: %s",
                            inventory.getMaterialId(),
                            inventory.getAvailableQuantity(),
                            safetyStock);
                    
                    createAlert(1, 2, "STOCK_INVENTORY", inventory.getId(), title, content);
                    alertCount++;
                }
            }
        }
        
        log.info("低库存预警检查完成，创建预警数: {}", alertCount);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkExpirationAlert() {
        log.info("开始检查有效期预警");
        
        LocalDate today = LocalDate.now();
        LocalDate alertDate = today.plusDays(30);
        
        // 查询30天内到期的库存
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(StockInventory::getExpireDate)
               .le(StockInventory::getExpireDate, alertDate)
               .ge(StockInventory::getExpireDate, today);
        
        List<StockInventory> expiringInventories = stockInventoryMapper.selectList(wrapper);
        
        int alertCount = 0;
        for (StockInventory inventory : expiringInventories) {
            // 检查是否已存在未处理的预警
            LambdaQueryWrapper<AlertRecord> alertWrapper = new LambdaQueryWrapper<>();
            alertWrapper.eq(AlertRecord::getAlertType, 2)
                       .eq(AlertRecord::getBusinessType, "STOCK_INVENTORY")
                       .eq(AlertRecord::getBusinessId, inventory.getId())
                       .eq(AlertRecord::getStatus, 1);
            
            if (alertRecordMapper.selectCount(alertWrapper) == 0) {
                long daysToExpire = java.time.temporal.ChronoUnit.DAYS.between(today, inventory.getExpireDate());
                
                String title = "有效期预警";
                String content = String.format("药品ID: %d, 批次号: %s, 有效期至: %s, 剩余天数: %d天",
                        inventory.getMaterialId(),
                        inventory.getBatchNumber(),
                        inventory.getExpireDate(),
                        daysToExpire);
                
                Integer alertLevel = daysToExpire <= 7 ? 3 : 2; // 7天内为严重，否则为警告
                createAlert(2, alertLevel, "STOCK_INVENTORY", inventory.getId(), title, content);
                alertCount++;
            }
        }
        
        log.info("有效期预警检查完成，创建预警数: {}", alertCount);
    }
}
