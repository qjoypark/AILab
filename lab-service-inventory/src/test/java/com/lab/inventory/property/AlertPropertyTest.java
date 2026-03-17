package com.lab.inventory.property;

import com.lab.inventory.entity.AlertRecord;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.AlertRecordMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.AlertService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 预警功能属性测试
 * 
 * 使用jqwik框架进行基于属性的测试，每个属性测试运行100次迭代
 */
@SpringBootTest
@Transactional
public class AlertPropertyTest {
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private AlertRecordMapper alertRecordMapper;
    
    @Autowired
    private StockInventoryMapper stockInventoryMapper;
    
    /**
     * 属性 8: 低库存自动预警
     * 
     * **Validates: Requirements 5.5**
     * 
     * 对于任何设置了安全库存的药品，当其可用库存数量低于安全库存阈值时，
     * 系统应自动创建预警记录并发送通知给相关管理员。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 8: 低库存自动预警")
    void lowStockAutoAlert(
            @ForAll @IntRange(min = 1, max = 99) int availableQuantity) {
        
        // 设置安全库存阈值为100（与AlertServiceImpl中的硬编码值一致）
        BigDecimal safetyStockThreshold = new BigDecimal("100");
        BigDecimal availableQty = new BigDecimal(availableQuantity);
        
        // 确保可用库存低于安全库存阈值
        Assume.that(availableQty.compareTo(safetyStockThreshold) < 0);
        
        // 创建库存记录
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(1L);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH" + System.currentTimeMillis());
        inventory.setQuantity(availableQty);
        inventory.setAvailableQuantity(availableQty);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(availableQty.multiply(BigDecimal.TEN));
        
        stockInventoryMapper.insert(inventory);
        
        // 执行低库存检查
        alertService.checkLowStockAlert();
        
        // 验证预警记录已创建
        List<AlertRecord> alerts = alertRecordMapper.selectList(null);
        
        // 应该至少有一条预警记录
        assertThat(alerts).isNotEmpty();
        
        // 验证预警记录的属性
        boolean hasLowStockAlert = alerts.stream()
                .anyMatch(alert -> 
                    alert.getAlertType() == 1 && // 低库存预警
                    alert.getBusinessType().equals("STOCK_INVENTORY") &&
                    alert.getBusinessId().equals(inventory.getId()) &&
                    alert.getStatus() == 1 // 未处理
                );
        
        assertThat(hasLowStockAlert).isTrue();
    }
}
