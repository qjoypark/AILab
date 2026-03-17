package com.lab.inventory.property;

import com.lab.inventory.entity.AlertRecord;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.AlertRecordMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.AlertService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 有效期预警属性测试
 * 
 * 使用jqwik框架进行基于属性的测试，每个属性测试运行100次迭代
 */
@SpringBootTest
@Transactional
public class ExpirationAlertPropertyTest {
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private AlertRecordMapper alertRecordMapper;
    
    @Autowired
    private StockInventoryMapper stockInventoryMapper;
    
    /**
     * 属性 11: 有效期预警及时性
     * 
     * **Validates: Requirements 5.8**
     * 
     * 对于任何有有效期的试剂，当当前日期距离有效期到期日期小于等于30天时，
     * 系统应创建有效期预警记录。
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 11: 有效期预警及时性")
    void expirationAlertTimeliness(
            @ForAll @IntRange(min = 1, max = 30) int daysToExpire,
            @ForAll @Positive BigDecimal quantity) {
        
        // 创建即将到期的库存记录
        LocalDate expireDate = LocalDate.now().plusDays(daysToExpire);
        
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(1L);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH" + System.currentTimeMillis());
        inventory.setQuantity(quantity);
        inventory.setAvailableQuantity(quantity);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setProductionDate(LocalDate.now().minusYears(1));
        inventory.setExpireDate(expireDate);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(quantity.multiply(BigDecimal.TEN));
        
        stockInventoryMapper.insert(inventory);
        
        // 执行有效期检查
        alertService.checkExpirationAlert();
        
        // 验证预警记录已创建
        List<AlertRecord> alerts = alertRecordMapper.selectList(null);
        
        // 应该至少有一条预警记录
        assertThat(alerts).isNotEmpty();
        
        // 验证预警记录的属性
        boolean hasExpirationAlert = alerts.stream()
                .anyMatch(alert -> 
                    alert.getAlertType() == 2 && // 有效期预警
                    alert.getBusinessType().equals("STOCK_INVENTORY") &&
                    alert.getBusinessId().equals(inventory.getId()) &&
                    alert.getStatus() == 1 // 未处理
                );
        
        assertThat(hasExpirationAlert).isTrue();
        
        // 验证预警级别
        AlertRecord expirationAlert = alerts.stream()
                .filter(alert -> alert.getAlertType() == 2 && 
                                alert.getBusinessId().equals(inventory.getId()))
                .findFirst()
                .orElse(null);
        
        if (expirationAlert != null) {
            // 7天内到期应为严重级别(3)，否则为警告级别(2)
            int expectedLevel = daysToExpire <= 7 ? 3 : 2;
            assertThat(expirationAlert.getAlertLevel()).isEqualTo(expectedLevel);
        }
    }
}
