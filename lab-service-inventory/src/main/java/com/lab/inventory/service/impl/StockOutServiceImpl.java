package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.annotation.AuditLog;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.HazardousUsageRecordDTO;
import com.lab.inventory.dto.MaterialApplicationDTO;
import com.lab.inventory.dto.MaterialApplicationItemDTO;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.dto.StockOutDTO;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.entity.StockOut;
import com.lab.inventory.entity.StockOutDetail;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.mapper.StockOutDetailMapper;
import com.lab.inventory.mapper.StockOutMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 出库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockOutServiceImpl implements com.lab.inventory.service.StockOutService {
    
    private final StockOutMapper stockOutMapper;
    private final StockOutDetailMapper stockOutDetailMapper;
    private final StockInventoryMapper stockInventoryMapper;
    private final ApprovalClient approvalClient;
    private final MaterialClient materialClient;
    
    @Override
    public Page<StockOut> listStockOut(int page, int size, Long warehouseId, Integer status) {
        Page<StockOut> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<>();
        
        if (warehouseId != null) {
            wrapper.eq(StockOut::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(StockOut::getStatus, status);
        }
        
        wrapper.orderByDesc(StockOut::getCreatedTime);
        return stockOutMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public StockOut getStockOutById(Long id) {
        StockOut stockOut = stockOutMapper.selectById(id);
        if (stockOut == null) {
            throw new BusinessException("出库单不存在");
        }
        return stockOut;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CREATE", businessType = "STOCK_OUT", description = "创建出库单")
    public StockOut createStockOut(StockOutDTO dto) {
        // 生成出库单号
        String outOrderNo = generateOutOrderNo();
        
        // 创建出库单
        StockOut stockOut = new StockOut();
        BeanUtils.copyProperties(dto, stockOut);
        stockOut.setOutOrderNo(outOrderNo);
        stockOut.setStatus(1); // 待出库
        
        stockOutMapper.insert(stockOut);
        
        // 创建出库明细
        for (StockOutDTO.StockOutDetailDTO itemDto : dto.getItems()) {
            StockOutDetail detail = new StockOutDetail();
            BeanUtils.copyProperties(itemDto, detail);
            detail.setOutOrderId(stockOut.getId());
            
            stockOutDetailMapper.insert(detail);
        }
        
        return stockOut;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CONFIRM", businessType = "STOCK_OUT", description = "确认出库")
    public void confirmStockOut(Long id) {
        StockOut stockOut = getStockOutById(id);
        
        if (stockOut.getStatus() != 1) {
            throw new BusinessException("出库单状态不允许确认");
        }
        
        // 查询出库明细
        LambdaQueryWrapper<StockOutDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockOutDetail::getOutOrderId, id);
        List<StockOutDetail> details = stockOutDetailMapper.selectList(wrapper);
        
        // 使用FIFO策略出库
        for (StockOutDetail detail : details) {
            decreaseInventoryFIFO(stockOut, detail);
        }
        
        // 更新出库单状态
        stockOut.setStatus(2); // 已出库
        stockOutMapper.updateById(stockOut);
        
        // 如果出库单关联了申请单，更新申请单状态为"已出库"
        if (stockOut.getApplicationId() != null) {
            try {
                approvalClient.updateApplicationStatusToStockOut(stockOut.getApplicationId());
                log.info("出库完成后更新申请单状态为已出库: applicationId={}, stockOutId={}", 
                    stockOut.getApplicationId(), id);
                
                // 获取申请单详情以创建危化品使用记录
                MaterialApplicationDTO application = approvalClient.getApplicationDetail(stockOut.getApplicationId());
                if (application != null) {
                    createHazardousUsageRecords(stockOut, application, details);
                }
            } catch (Exception e) {
                log.error("更新申请单状态失败: applicationId={}, stockOutId={}", 
                    stockOut.getApplicationId(), id, e);
                // 不影响出库流程，只记录日志
            }
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CREATE_FROM_APPLICATION", businessType = "STOCK_OUT", 
              description = "根据申请单创建出库单")
    public StockOut createStockOutFromApplication(Long applicationId) {
        log.info("根据申请单创建出库单: applicationId={}", applicationId);
        
        // 1. 获取申请单详情
        MaterialApplicationDTO application = approvalClient.getApplicationDetail(applicationId);
        if (application == null) {
            throw new BusinessException("申请单不存在");
        }
        
        // 2. 验证申请单状态（必须是审批通过状态）
        if (application.getStatus() != 3) {
            throw new BusinessException("申请单状态不是审批通过，无法创建出库单");
        }
        
        // 3. 检查是否已经创建过出库单
        LambdaQueryWrapper<StockOut> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(StockOut::getApplicationId, applicationId);
        checkWrapper.ne(StockOut::getStatus, 3); // 排除已取消的
        Long existingCount = stockOutMapper.selectCount(checkWrapper);
        if (existingCount > 0) {
            throw new BusinessException("该申请单已创建出库单");
        }
        
        // 4. 创建出库单
        String outOrderNo = generateOutOrderNo();
        
        StockOut stockOut = new StockOut();
        stockOut.setOutOrderNo(outOrderNo);
        stockOut.setOutType(1); // 1-领用出库
        stockOut.setWarehouseId(1L); // TODO: 根据实际情况选择仓库
        stockOut.setApplicationId(applicationId);
        stockOut.setReceiverId(application.getApplicantId());
        stockOut.setReceiverName(application.getApplicantName());
        stockOut.setReceiverDept(application.getApplicantDept());
        stockOut.setOutDate(LocalDate.now());
        stockOut.setOperatorId(1L); // TODO: 获取当前操作人ID
        stockOut.setStatus(1); // 待出库
        stockOut.setRemark("根据申请单 " + application.getApplicationNo() + " 自动创建");
        
        stockOutMapper.insert(stockOut);
        
        // 5. 创建出库明细
        if (application.getItems() != null && !application.getItems().isEmpty()) {
            for (MaterialApplicationItemDTO item : application.getItems()) {
                StockOutDetail detail = new StockOutDetail();
                detail.setOutOrderId(stockOut.getId());
                detail.setMaterialId(item.getMaterialId());
                // 使用批准数量，如果没有批准数量则使用申请数量
                BigDecimal quantity = item.getApprovedQuantity() != null ? 
                    item.getApprovedQuantity() : item.getApplyQuantity();
                detail.setQuantity(quantity);
                
                stockOutDetailMapper.insert(detail);
            }
        }
        
        log.info("根据申请单创建出库单成功: applicationId={}, stockOutId={}, outOrderNo={}", 
            applicationId, stockOut.getId(), outOrderNo);
        
        return stockOut;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CANCEL", businessType = "STOCK_OUT", description = "取消出库单")
    public void cancelStockOut(Long id) {
        StockOut stockOut = getStockOutById(id);
        
        if (stockOut.getStatus() != 1) {
            throw new BusinessException("出库单状态不允许取消");
        }
        
        stockOut.setStatus(3); // 已取消
        stockOutMapper.updateById(stockOut);
    }
    
    /**
     * 使用FIFO策略减少库存
     */
    private void decreaseInventoryFIFO(StockOut stockOut, StockOutDetail detail) {
        // 查询该药品在该仓库的所有批次库存，按生产日期排序（FIFO）
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInventory::getMaterialId, detail.getMaterialId());
        wrapper.eq(StockInventory::getWarehouseId, stockOut.getWarehouseId());
        wrapper.gt(StockInventory::getAvailableQuantity, BigDecimal.ZERO);
        wrapper.orderByAsc(StockInventory::getProductionDate);
        
        List<StockInventory> inventories = stockInventoryMapper.selectList(wrapper);
        
        if (inventories.isEmpty()) {
            throw new BusinessException("库存不足");
        }
        
        BigDecimal remainingQuantity = detail.getQuantity();
        
        for (StockInventory inventory : inventories) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            BigDecimal availableQty = inventory.getAvailableQuantity();
            BigDecimal deductQty = remainingQuantity.min(availableQty);
            
            inventory.setQuantity(inventory.getQuantity().subtract(deductQty));
            inventory.setAvailableQuantity(inventory.getAvailableQuantity().subtract(deductQty));
            
            if (inventory.getUnitPrice() != null) {
                inventory.setTotalAmount(inventory.getQuantity().multiply(inventory.getUnitPrice()));
            }
            
            stockInventoryMapper.updateById(inventory);
            
            remainingQuantity = remainingQuantity.subtract(deductQty);
        }
        
        if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("库存不足，缺少数量: " + remainingQuantity);
        }
    }
    
    /**
     * 为危化品出库创建使用记录
     */
    private void createHazardousUsageRecords(StockOut stockOut, MaterialApplicationDTO application, 
                                            List<StockOutDetail> details) {
        log.info("检查是否需要创建危化品使用记录: applicationId={}, stockOutId={}", 
            application.getId(), stockOut.getId());
        
        for (StockOutDetail detail : details) {
            try {
                // 获取药品信息
                MaterialInfo materialInfo = materialClient.getMaterialInfo(detail.getMaterialId());
                if (materialInfo == null) {
                    log.warn("无法获取药品信息，跳过创建使用记录: materialId={}", detail.getMaterialId());
                    continue;
                }
                
                // 检查是否为危化品（materialType=3 或 isControlled=1/2）
                boolean isHazardous = (materialInfo.getMaterialType() != null && materialInfo.getMaterialType() == 3) ||
                                     (materialInfo.getIsControlled() != null && materialInfo.getIsControlled() > 0);
                
                if (isHazardous) {
                    log.info("检测到危化品出库，创建使用记录: materialId={}, materialName={}", 
                        detail.getMaterialId(), materialInfo.getMaterialName());
                    
                    // 创建危化品使用记录
                    HazardousUsageRecordDTO recordDTO = new HazardousUsageRecordDTO();
                    recordDTO.setApplicationId(application.getId());
                    recordDTO.setApplicationNo(application.getApplicationNo());
                    recordDTO.setMaterialId(detail.getMaterialId());
                    recordDTO.setMaterialName(materialInfo.getMaterialName());
                    recordDTO.setUserId(stockOut.getReceiverId());
                    recordDTO.setUserName(stockOut.getReceiverName());
                    recordDTO.setReceivedQuantity(detail.getQuantity());
                    recordDTO.setUsageDate(stockOut.getOutDate());
                    recordDTO.setUsageLocation(application.getUsageLocation());
                    recordDTO.setUsagePurpose(application.getUsagePurpose());
                    
                    approvalClient.createHazardousUsageRecord(recordDTO);
                    
                    log.info("危化品使用记录创建成功: materialId={}, applicationId={}", 
                        detail.getMaterialId(), application.getId());
                } else {
                    log.debug("非危化品，无需创建使用记录: materialId={}, materialType={}, isControlled={}", 
                        detail.getMaterialId(), materialInfo.getMaterialType(), materialInfo.getIsControlled());
                }
            } catch (Exception e) {
                log.error("创建危化品使用记录失败: materialId={}, applicationId={}", 
                    detail.getMaterialId(), application.getId(), e);
                // 不抛出异常，避免影响出库流程
            }
        }
    }
    
    /**
     * 生成出库单号
     */
    private String generateOutOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "OUT" + date;
        
        // 查询当天最大单号
        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(StockOut::getOutOrderNo, prefix);
        wrapper.orderByDesc(StockOut::getOutOrderNo);
        wrapper.last("LIMIT 1");
        
        StockOut lastOrder = stockOutMapper.selectOne(wrapper);
        
        int sequence = 1;
        if (lastOrder != null) {
            String lastNo = lastOrder.getOutOrderNo();
            sequence = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        
        return prefix + String.format("%04d", sequence);
    }
}
