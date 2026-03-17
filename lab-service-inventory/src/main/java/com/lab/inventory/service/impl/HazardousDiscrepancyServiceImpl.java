package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.AlertService;
import com.lab.inventory.service.HazardousDiscrepancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 危化品账实差异服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HazardousDiscrepancyServiceImpl implements HazardousDiscrepancyService {
    
    private final MaterialClient materialClient;
    private final ApprovalClient approvalClient;
    private final StockInventoryMapper stockInventoryMapper;
    private final AlertService alertService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateDiscrepancy() {
        log.info("开始计算危化品账实差异");
        
        // 1. 获取所有危化品列表
        List<MaterialInfo> hazardousMaterials = materialClient.getHazardousMaterials();
        
        if (hazardousMaterials.isEmpty()) {
            log.info("没有危化品需要计算账实差异");
            return;
        }
        
        log.info("获取到危化品数量: {}", hazardousMaterials.size());
        
        int alertCount = 0;
        int processedCount = 0;
        
        // 2. 遍历每个危化品，计算账实差异
        for (MaterialInfo material : hazardousMaterials) {
            try {
                processedCount++;
                
                // 2.1 获取账面库存（从stock_inventory表汇总）
                LambdaQueryWrapper<StockInventory> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(StockInventory::getMaterialId, material.getId());
                
                List<StockInventory> inventories = stockInventoryMapper.selectList(wrapper);
                
                // 计算总账面库存
                BigDecimal bookStock = inventories.stream()
                        .map(StockInventory::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                // 如果账面库存为0，跳过
                if (bookStock.compareTo(BigDecimal.ZERO) == 0) {
                    log.debug("危化品 {} 账面库存为0，跳过计算", material.getMaterialName());
                    continue;
                }
                
                // 2.2 获取已领用未归还数量（从hazardous_usage_record表查询status=1的记录）
                BigDecimal unreturnedQuantity = approvalClient.getUnreturnedQuantity(material.getId());
                
                // 2.3 计算实际库存 = 账面库存 - 已领用未归还数量
                BigDecimal actualStock = bookStock.subtract(unreturnedQuantity);
                
                // 2.4 计算账实差异 = (账面库存 - 实际库存) / 账面库存 × 100%
                BigDecimal discrepancy = bookStock.subtract(actualStock)
                        .divide(bookStock, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                
                log.info("危化品账实差异计算: 药品={}, 账面库存={}, 已领用未归还={}, 实际库存={}, 差异={}%",
                        material.getMaterialName(), bookStock, unreturnedQuantity, actualStock, 
                        discrepancy.setScale(2, RoundingMode.HALF_UP));
                
                // 2.5 如果差异绝对值超过5%，触发预警
                BigDecimal absDiscrepancy = discrepancy.abs();
                if (absDiscrepancy.compareTo(new BigDecimal("5")) > 0) {
                    String title = "危化品账实差异预警";
                    String content = String.format(
                            "危化品: %s\n" +
                            "账面库存: %s %s\n" +
                            "已领用未归还: %s %s\n" +
                            "实际库存: %s %s\n" +
                            "账实差异: %s%%\n" +
                            "预警原因: 账实差异绝对值超过5%%",
                            material.getMaterialName(),
                            bookStock.setScale(2, RoundingMode.HALF_UP), material.getUnit(),
                            unreturnedQuantity.setScale(2, RoundingMode.HALF_UP), material.getUnit(),
                            actualStock.setScale(2, RoundingMode.HALF_UP), material.getUnit(),
                            discrepancy.setScale(2, RoundingMode.HALF_UP)
                    );
                    
                    // 创建预警记录
                    // alertType: 4-账实差异, alertLevel: 3-严重
                    alertService.createAlert(4, 3, "HAZARDOUS_MATERIAL", material.getId(), title, content);
                    alertCount++;
                    
                    log.warn("危化品账实差异超标: 药品={}, 差异={}%", material.getMaterialName(), 
                            discrepancy.setScale(2, RoundingMode.HALF_UP));
                }
                
            } catch (Exception e) {
                log.error("计算危化品账实差异失败: materialId={}, materialName={}", 
                        material.getId(), material.getMaterialName(), e);
                // 继续处理下一个危化品
            }
        }
        
        log.info("危化品账实差异计算完成，处理数量: {}, 创建预警数: {}", processedCount, alertCount);
    }
}
