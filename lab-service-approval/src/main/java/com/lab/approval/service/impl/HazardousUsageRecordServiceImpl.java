package com.lab.approval.service.impl;

import com.lab.approval.client.InventoryClient;
import com.lab.approval.dto.HazardousReturnRequest;
import com.lab.approval.entity.HazardousUsageRecord;
import com.lab.approval.mapper.HazardousUsageRecordMapper;
import com.lab.approval.service.HazardousUsageRecordService;
import com.lab.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 危化品使用记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HazardousUsageRecordServiceImpl implements HazardousUsageRecordService {
    
    private final HazardousUsageRecordMapper usageRecordMapper;
    private final InventoryClient inventoryClient;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HazardousUsageRecord createUsageRecord(HazardousUsageRecord record) {
        log.info("创建危化品使用记录: applicationId={}, materialId={}, userId={}", 
            record.getApplicationId(), record.getMaterialId(), record.getUserId());
        
        usageRecordMapper.insert(record);
        
        log.info("危化品使用记录创建成功: recordId={}", record.getId());
        return record;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HazardousUsageRecord returnHazardousMaterial(Long recordId, HazardousReturnRequest returnRequest) {
        log.info("危化品归还: recordId={}, actualUsed={}, returned={}, waste={}", 
            recordId, returnRequest.getActualUsedQuantity(), 
            returnRequest.getReturnedQuantity(), returnRequest.getWasteQuantity());
        
        // 1. 查询使用记录
        HazardousUsageRecord record = usageRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("使用记录不存在");
        }
        
        // 2. 验证记录状态
        if (record.getStatus() != 1) {
            throw new BusinessException("该记录已归还，不能重复归还");
        }
        
        // 3. 验证数量关系：领用数量 = 实际使用量 + 归还量 + 废弃量
        BigDecimal totalQuantity = returnRequest.getActualUsedQuantity()
            .add(returnRequest.getReturnedQuantity())
            .add(returnRequest.getWasteQuantity());
        
        if (totalQuantity.compareTo(record.getReceivedQuantity()) != 0) {
            throw new BusinessException(
                String.format("数量不匹配：实际使用量(%.2f) + 归还量(%.2f) + 废弃量(%.2f) = %.2f，应等于领用数量(%.2f)",
                    returnRequest.getActualUsedQuantity(),
                    returnRequest.getReturnedQuantity(),
                    returnRequest.getWasteQuantity(),
                    totalQuantity,
                    record.getReceivedQuantity())
            );
        }
        
        // 4. 如果有归还数量，调用库存服务更新库存
        if (returnRequest.getReturnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            boolean success = inventoryClient.returnHazardousMaterial(
                record.getMaterialId(),
                returnRequest.getReturnedQuantity(),
                "危化品归还入库 - 使用记录ID: " + recordId
            );
            
            if (!success) {
                log.warn("调用库存服务归还入库失败，但继续更新使用记录: recordId={}", recordId);
                // 注意：这里不抛出异常，因为使用记录的更新比库存更新更重要
                // 库存可以后续通过盘点等方式调整
            }
        }
        
        // 5. 更新使用记录
        record.setActualUsedQuantity(returnRequest.getActualUsedQuantity());
        record.setReturnedQuantity(returnRequest.getReturnedQuantity());
        record.setWasteQuantity(returnRequest.getWasteQuantity());
        record.setReturnDate(LocalDate.now());
        record.setStatus(2); // 状态更新为"已归还"
        
        // 如果有备注，追加到原有备注
        if (returnRequest.getRemark() != null && !returnRequest.getRemark().isEmpty()) {
            String existingRemark = record.getRemark() != null ? record.getRemark() : "";
            record.setRemark(existingRemark.isEmpty() 
                ? returnRequest.getRemark() 
                : existingRemark + "; " + returnRequest.getRemark());
        }
        
        usageRecordMapper.updateById(record);
        
        log.info("危化品归还成功: recordId={}, status={}", recordId, record.getStatus());
        return record;
    }
    
    @Override
    public BigDecimal getUnreturnedQuantity(Long materialId) {
        log.info("获取危化品已领用未归还数量: materialId={}", materialId);
        
        // 查询status=1(使用中)的记录，汇总receivedQuantity
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HazardousUsageRecord> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(HazardousUsageRecord::getMaterialId, materialId)
               .eq(HazardousUsageRecord::getStatus, 1); // 1-使用中
        
        java.util.List<HazardousUsageRecord> records = usageRecordMapper.selectList(wrapper);
        
        BigDecimal totalUnreturned = records.stream()
            .map(HazardousUsageRecord::getReceivedQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("危化品已领用未归还数量: materialId={}, quantity={}", materialId, totalUnreturned);
        return totalUnreturned;
    }
}
