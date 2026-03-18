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
import com.lab.inventory.dto.StockOutOrderSummaryDTO;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.entity.StockOut;
import com.lab.inventory.entity.StockOutDetail;
import com.lab.inventory.entity.Warehouse;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.mapper.StockOutDetailMapper;
import com.lab.inventory.mapper.StockOutMapper;
import com.lab.inventory.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final WarehouseMapper warehouseMapper;
    private final ApprovalClient approvalClient;
    private final MaterialClient materialClient;

    private static final Long DEFAULT_SYSTEM_OPERATOR_ID = 1L;
    
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

        LambdaQueryWrapper<StockOutDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(StockOutDetail::getOutOrderId, id);
        detailWrapper.orderByAsc(StockOutDetail::getId);
        List<StockOutDetail> details = stockOutDetailMapper.selectList(detailWrapper);
        stockOut.setItems(details);

        return stockOut;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(operationType = "CREATE", businessType = "STOCK_OUT", description = "创建出库单")
    public StockOut createStockOut(StockOutDTO dto) {
        // 生成出库单号
        String outOrderNo = generateOutOrderNo();
        LocalDateTime now = LocalDateTime.now();
        
        // 创建出库单
        StockOut stockOut = new StockOut();
        BeanUtils.copyProperties(dto, stockOut);
        stockOut.setOutOrderNo(outOrderNo);
        stockOut.setStatus(1); // 待出库
        stockOut.setCreatedBy(dto.getOperatorId());
        stockOut.setCreatedTime(now);
        stockOut.setUpdatedBy(dto.getOperatorId());
        stockOut.setUpdatedTime(now);
        
        stockOutMapper.insert(stockOut);
        
        // 创建出库明细
        for (StockOutDTO.StockOutDetailDTO itemDto : dto.getItems()) {
            StockOutDetail detail = new StockOutDetail();
            BeanUtils.copyProperties(itemDto, detail);
            detail.setOutOrderId(stockOut.getId());
            detail.setCreatedTime(now);
            
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
        stockOut.setUpdatedBy(stockOut.getOperatorId());
        stockOut.setUpdatedTime(LocalDateTime.now());
        stockOutMapper.updateById(stockOut);
        
        // 如果出库单关联了申请单，处理申请状态和危化品记录
        if (stockOut.getApplicationId() != null) {
            try {
                // 获取申请单详情以创建危化品使用记录
                MaterialApplicationDTO application = approvalClient.getApplicationDetail(stockOut.getApplicationId());
                if (application != null) {
                    createHazardousUsageRecords(stockOut, application, details);
                }

                // 仅当同申请下所有出库单均已完成时，回写申请状态为已出库
                if (isAllStockOutCompleted(stockOut.getApplicationId())) {
                    approvalClient.updateApplicationStatusToStockOut(stockOut.getApplicationId());
                    log.info("同申请出库单均完成，更新申请状态为已出库: applicationId={}", stockOut.getApplicationId());
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
        if (application.getStatus() != null && application.getStatus() != 4 && application.getStatus() != 7) {
            application.setStatus(3);
        }
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
        
        if (application.getItems() == null || application.getItems().isEmpty()) {
            throw new BusinessException("申请单明细为空，无法创建出库单");
        }

        // 4. 按总库存自动分配：支持跨仓库/库位拆分
        Map<Long, List<StockAllocation>> allocationByWarehouse = new LinkedHashMap<>();
        for (MaterialApplicationItemDTO item : application.getItems()) {
            BigDecimal requiredQuantity = item.getApprovedQuantity() != null
                    ? item.getApprovedQuantity()
                    : item.getApplyQuantity();
            if (requiredQuantity == null || requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LambdaQueryWrapper<StockInventory> stockWrapper = new LambdaQueryWrapper<>();
            stockWrapper.eq(StockInventory::getMaterialId, item.getMaterialId());
            stockWrapper.gt(StockInventory::getAvailableQuantity, BigDecimal.ZERO);
            stockWrapper.orderByAsc(StockInventory::getWarehouseId);
            stockWrapper.orderByAsc(StockInventory::getProductionDate);
            stockWrapper.orderByAsc(StockInventory::getId);
            List<StockInventory> inventories = stockInventoryMapper.selectList(stockWrapper);
            if (inventories.isEmpty()) {
                throw new BusinessException("药品库存不足，无法创建出库单: " + item.getMaterialName());
            }

            BigDecimal totalAvailable = inventories.stream()
                    .map(inventory -> inventory.getAvailableQuantity() != null ? inventory.getAvailableQuantity() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalAvailable.compareTo(requiredQuantity) < 0) {
                throw new BusinessException("药品库存不足，药品: " + item.getMaterialName()
                        + "，可用: " + totalAvailable + "，申请: " + requiredQuantity);
            }

            BigDecimal remaining = requiredQuantity;
            for (StockInventory inventory : inventories) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal available = inventory.getAvailableQuantity() != null ? inventory.getAvailableQuantity() : BigDecimal.ZERO;
                if (available.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal splitQuantity = remaining.min(available);
                StockAllocation allocation = new StockAllocation();
                allocation.setWarehouseId(inventory.getWarehouseId());
                allocation.setMaterialId(item.getMaterialId());
                allocation.setBatchNumber(inventory.getBatchNumber());
                allocation.setStorageLocationId(inventory.getStorageLocationId());
                allocation.setQuantity(splitQuantity);
                allocation.setUnitPrice(inventory.getUnitPrice());
                allocationByWarehouse
                        .computeIfAbsent(inventory.getWarehouseId(), key -> new ArrayList<>())
                        .add(allocation);

                remaining = remaining.subtract(splitQuantity);
            }
        }

        if (allocationByWarehouse.isEmpty()) {
            throw new BusinessException("未生成出库分配明细，无法创建出库单");
        }

        // 5. 按仓库生成多个待出库单，供实验员执行
        LocalDateTime now = LocalDateTime.now();
        List<StockOut> createdOrders = new ArrayList<>();
        for (Map.Entry<Long, List<StockAllocation>> entry : allocationByWarehouse.entrySet()) {
            Long warehouseId = entry.getKey();
            List<StockAllocation> allocations = entry.getValue();

            StockOut stockOut = new StockOut();
            stockOut.setOutOrderNo(generateOutOrderNo());
            stockOut.setOutType(1); // 1-领用出库
            stockOut.setWarehouseId(warehouseId);
            stockOut.setApplicationId(applicationId);
            stockOut.setReceiverId(application.getApplicantId());
            stockOut.setReceiverName(application.getApplicantName());
            stockOut.setReceiverDept(application.getApplicantDept());
            stockOut.setOutDate(LocalDate.now());
            stockOut.setOperatorId(DEFAULT_SYSTEM_OPERATOR_ID);
            stockOut.setStatus(1); // 待出库
            stockOut.setRemark("审批通过自动拆分出库，申请单: " + application.getApplicationNo());
            stockOut.setCreatedBy(stockOut.getOperatorId());
            stockOut.setCreatedTime(now);
            stockOut.setUpdatedBy(stockOut.getOperatorId());
            stockOut.setUpdatedTime(now);
            stockOutMapper.insert(stockOut);

            for (StockAllocation allocation : allocations) {
                StockOutDetail detail = new StockOutDetail();
                detail.setOutOrderId(stockOut.getId());
                detail.setMaterialId(allocation.getMaterialId());
                detail.setBatchNumber(allocation.getBatchNumber());
                detail.setStorageLocationId(allocation.getStorageLocationId());
                detail.setQuantity(allocation.getQuantity());
                detail.setUnitPrice(allocation.getUnitPrice());
                if (allocation.getUnitPrice() != null) {
                    detail.setTotalAmount(allocation.getUnitPrice().multiply(allocation.getQuantity()));
                }
                detail.setCreatedTime(now);
                stockOutDetailMapper.insert(detail);
            }

            createdOrders.add(stockOut);
        }

        StockOut firstOrder = createdOrders.get(0);
        log.info("根据申请单创建出库单成功: applicationId={}, orderCount={}, firstOrderId={}",
                applicationId, createdOrders.size(), firstOrder.getId());
        return firstOrder;
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
        stockOut.setUpdatedBy(stockOut.getOperatorId());
        stockOut.setUpdatedTime(LocalDateTime.now());
        stockOutMapper.updateById(stockOut);
    }

    @Override
    public List<StockOutOrderSummaryDTO> listStockOutByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockOut::getApplicationId, applicationId)
                .orderByAsc(StockOut::getCreatedTime)
                .orderByAsc(StockOut::getId);
        List<StockOut> stockOutList = stockOutMapper.selectList(wrapper);
        if (stockOutList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> warehouseIds = stockOutList.stream()
                .map(StockOut::getWarehouseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> warehouseNameMap = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            LambdaQueryWrapper<Warehouse> warehouseWrapper = new LambdaQueryWrapper<>();
            warehouseWrapper.in(Warehouse::getId, warehouseIds);
            List<Warehouse> warehouseList = warehouseMapper.selectList(warehouseWrapper);
            for (Warehouse warehouse : warehouseList) {
                warehouseNameMap.put(warehouse.getId(), warehouse.getWarehouseName());
            }
        }

        List<StockOutOrderSummaryDTO> result = new ArrayList<>(stockOutList.size());
        for (StockOut stockOut : stockOutList) {
            StockOutOrderSummaryDTO summary = new StockOutOrderSummaryDTO();
            summary.setId(stockOut.getId());
            summary.setOutOrderNo(stockOut.getOutOrderNo());
            summary.setWarehouseId(stockOut.getWarehouseId());
            String fallbackWarehouseName = stockOut.getWarehouseId() == null
                    ? "未知仓库"
                    : stockOut.getWarehouseId() + "号仓库";
            summary.setWarehouseName(warehouseNameMap.getOrDefault(stockOut.getWarehouseId(), fallbackWarehouseName));
            summary.setStatus(stockOut.getStatus());
            summary.setStatusName(resolveStockOutStatusName(stockOut.getStatus()));
            summary.setCreatedTime(stockOut.getCreatedTime());
            result.add(summary);
        }
        return result;
    }
    
    /**
     * 使用FIFO策略减少库存
     */
    private void decreaseInventoryFIFO(StockOut stockOut, StockOutDetail detail) {
        LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInventory::getMaterialId, detail.getMaterialId());
        wrapper.eq(StockInventory::getWarehouseId, stockOut.getWarehouseId());
        wrapper.gt(StockInventory::getAvailableQuantity, BigDecimal.ZERO);

        if (StringUtils.hasText(detail.getBatchNumber())) {
            wrapper.eq(StockInventory::getBatchNumber, detail.getBatchNumber());
        }
        if (detail.getStorageLocationId() != null) {
            wrapper.eq(StockInventory::getStorageLocationId, detail.getStorageLocationId());
        }

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

    private boolean isAllStockOutCompleted(Long applicationId) {
        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockOut::getApplicationId, applicationId);
        wrapper.ne(StockOut::getStatus, 3); // 排除已取消
        List<StockOut> stockOutList = stockOutMapper.selectList(wrapper);
        if (stockOutList.isEmpty()) {
            return false;
        }
        return stockOutList.stream().allMatch(stockOut -> stockOut.getStatus() != null && stockOut.getStatus() == 2);
    }

    private String resolveStockOutStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "待出库";
            case 2:
                return "已出库";
            case 3:
                return "已取消";
            default:
                return "未知";
        }
    }

    @lombok.Data
    private static class StockAllocation {
        private Long warehouseId;
        private Long materialId;
        private String batchNumber;
        private Long storageLocationId;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
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
