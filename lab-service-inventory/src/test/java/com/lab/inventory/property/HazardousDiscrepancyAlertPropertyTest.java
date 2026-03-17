package com.lab.inventory.property;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.entity.AlertRecord;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.AlertRecordMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.AlertService;
import com.lab.inventory.service.impl.HazardousDiscrepancyServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.Positive;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 危化品账实差异预警属性测试
 * 
 * 使用jqwik框架进行基于属性的测试，每个属性测试运行100次迭代
 */
@SpringBootTest
@Transactional
public class HazardousDiscrepancyAlertPropertyTest {
    
    @Autowired
    private StockInventoryMapper stockInventoryMapper;
    
    @Autowired
    private AlertRecordMapper alertRecordMapper;
    
    @MockBean
    private MaterialClient materialClient;
    
    @MockBean
    private ApprovalClient approvalClient;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private HazardousDiscrepancyServiceImpl hazardousDiscrepancyService;
    
    /**
     * 属性 16: 危化品账实差异预警
     * 
     * **Validates: Requirements 6.8**
     * 
     * 对于任何危化品，当其账实差异的绝对值超过5%时，系统应创建异常预警记录，
     * 预警级别为"严重"（level=3）。
     * 
     * 测试策略：
     * - 生成随机的账面库存和已领用未归还数量
     * - 计算账实差异百分比
     * - 验证当差异绝对值>5%时，创建预警记录
     * - 验证预警级别为"严重"（3）
     * - 验证预警类型为"账实差异"（4）
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 16: 危化品账实差异预警")
    void hazardousDiscrepancyAlertWhenExceeds5Percent(
            @ForAll @Positive @BigRange(min = "10", max = "10000") BigDecimal bookStock,
            @ForAll @BigRange(min = "-1000", max = "1000") BigDecimal unreturnedQuantity) {
        
        // 前置条件：确保已领用未归还数量在合理范围内
        // 允许负数（归还多了）和正数（未归还）
        Assume.that(unreturnedQuantity.abs().compareTo(bookStock) <= 0);
        
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis() + (long)(Math.random() * 1000);
        
        // 创建危化品信息
        MaterialInfo material = new MaterialInfo();
        material.setId(materialId);
        material.setMaterialName("测试危化品-" + materialId);
        material.setMaterialType(3); // 3-危化品
        material.setUnit("kg");
        material.setCasNumber("CAS-" + materialId);
        material.setDangerCategory("易燃液体");
        material.setIsControlled(1);
        
        // 创建库存记录
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(materialId);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH-" + materialId);
        inventory.setQuantity(bookStock);
        inventory.setAvailableQuantity(bookStock);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(bookStock.multiply(BigDecimal.TEN));
        inventory.setProductionDate(LocalDate.now());
        inventory.setExpireDate(LocalDate.now().plusYears(1));
        
        stockInventoryMapper.insert(inventory);
        
        // 模拟MaterialClient返回危化品列表
        when(materialClient.getHazardousMaterials())
                .thenReturn(Collections.singletonList(material));
        
        // 模拟ApprovalClient返回已领用未归还数量
        when(approvalClient.getUnreturnedQuantity(materialId))
                .thenReturn(unreturnedQuantity);
        
        // 2. 计算预期的账实差异
        BigDecimal actualStock = bookStock.subtract(unreturnedQuantity);
        BigDecimal discrepancy = bookStock.subtract(actualStock)
                .divide(bookStock, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        BigDecimal discrepancyAbs = discrepancy.abs();
        
        // 3. 执行账实差异计算和预警检查
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // 4. 查询是否创建了预警记录
        LambdaQueryWrapper<AlertRecord> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.eq(AlertRecord::getBusinessType, "HAZARDOUS_MATERIAL")
                .eq(AlertRecord::getBusinessId, materialId)
                .eq(AlertRecord::getAlertType, 4); // 4-账实差异
        
        List<AlertRecord> alerts = alertRecordMapper.selectList(alertWrapper);
        
        // 5. 验证预警创建逻辑
        if (discrepancyAbs.compareTo(new BigDecimal("5")) > 0) {
            // 差异绝对值>5%，应该创建预警
            assertThat(alerts)
                    .as("当账实差异绝对值>5%%时，应创建预警记录")
                    .isNotEmpty();
            
            AlertRecord alert = alerts.get(0);
            
            // 验证预警类型为账实差异（4）
            assertThat(alert.getAlertType())
                    .as("预警类型应为账实差异（4）")
                    .isEqualTo(4);
            
            // 验证预警级别为严重（3）
            assertThat(alert.getAlertLevel())
                    .as("预警级别应为严重（3）")
                    .isEqualTo(3);
            
            // 验证业务类型
            assertThat(alert.getBusinessType())
                    .as("业务类型应为HAZARDOUS_MATERIAL")
                    .isEqualTo("HAZARDOUS_MATERIAL");
            
            // 验证业务ID
            assertThat(alert.getBusinessId())
                    .as("业务ID应为药品ID")
                    .isEqualTo(materialId);
            
            // 验证预警标题
            assertThat(alert.getAlertTitle())
                    .as("预警标题应为'危化品账实差异预警'")
                    .isEqualTo("危化品账实差异预警");
            
            // 验证预警内容包含关键信息
            String content = alert.getAlertContent();
            assertThat(content)
                    .as("预警内容应包含药品名称")
                    .contains(material.getMaterialName());
            
            assertThat(content)
                    .as("预警内容应包含账面库存")
                    .contains(bookStock.toString());
            
            // 验证预警状态为未处理（1）
            assertThat(alert.getStatus())
                    .as("预警状态应为未处理（1）")
                    .isEqualTo(1);
            
        } else {
            // 差异绝对值<=5%，不应该创建预警
            assertThat(alerts)
                    .as("当账实差异绝对值<=5%%时，不应创建预警记录")
                    .isEmpty();
        }
    }
    
    /**
     * 属性 16 边界情况测试：差异恰好为5%
     * 
     * **Validates: Requirements 6.8**
     * 
     * 当账实差异恰好为5%时，不应创建预警（需要严格大于5%）
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 16: 危化品账实差异预警")
    void hazardousDiscrepancyAlertBoundaryAt5Percent(
            @ForAll @Positive @BigRange(min = "100", max = "10000") BigDecimal bookStock) {
        
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis() + (long)(Math.random() * 1000);
        
        // 计算恰好5%差异的已领用未归还数量
        BigDecimal unreturnedQuantity = bookStock.multiply(new BigDecimal("0.05"));
        
        // 创建危化品信息
        MaterialInfo material = new MaterialInfo();
        material.setId(materialId);
        material.setMaterialName("测试危化品-" + materialId);
        material.setMaterialType(3);
        material.setUnit("kg");
        material.setCasNumber("CAS-" + materialId);
        material.setDangerCategory("易燃液体");
        material.setIsControlled(1);
        
        // 创建库存记录
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(materialId);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH-" + materialId);
        inventory.setQuantity(bookStock);
        inventory.setAvailableQuantity(bookStock);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(bookStock.multiply(BigDecimal.TEN));
        inventory.setProductionDate(LocalDate.now());
        inventory.setExpireDate(LocalDate.now().plusYears(1));
        
        stockInventoryMapper.insert(inventory);
        
        // 模拟MaterialClient返回危化品列表
        when(materialClient.getHazardousMaterials())
                .thenReturn(Collections.singletonList(material));
        
        // 模拟ApprovalClient返回已领用未归还数量
        when(approvalClient.getUnreturnedQuantity(materialId))
                .thenReturn(unreturnedQuantity);
        
        // 2. 执行账实差异计算和预警检查
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // 3. 查询是否创建了预警记录
        LambdaQueryWrapper<AlertRecord> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.eq(AlertRecord::getBusinessType, "HAZARDOUS_MATERIAL")
                .eq(AlertRecord::getBusinessId, materialId)
                .eq(AlertRecord::getAlertType, 4);
        
        List<AlertRecord> alerts = alertRecordMapper.selectList(alertWrapper);
        
        // 4. 验证：差异恰好为5%时，不应创建预警
        assertThat(alerts)
                .as("当账实差异恰好为5%%时，不应创建预警记录")
                .isEmpty();
    }
    
    /**
     * 属性 16 边界情况测试：差异略大于5%
     * 
     * **Validates: Requirements 6.8**
     * 
     * 当账实差异略大于5%时（如5.01%），应创建预警
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 16: 危化品账实差异预警")
    void hazardousDiscrepancyAlertJustAbove5Percent(
            @ForAll @Positive @BigRange(min = "100", max = "10000") BigDecimal bookStock) {
        
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis() + (long)(Math.random() * 1000);
        
        // 计算略大于5%差异的已领用未归还数量（5.1%）
        BigDecimal unreturnedQuantity = bookStock.multiply(new BigDecimal("0.051"));
        
        // 创建危化品信息
        MaterialInfo material = new MaterialInfo();
        material.setId(materialId);
        material.setMaterialName("测试危化品-" + materialId);
        material.setMaterialType(3);
        material.setUnit("kg");
        material.setCasNumber("CAS-" + materialId);
        material.setDangerCategory("易燃液体");
        material.setIsControlled(1);
        
        // 创建库存记录
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(materialId);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH-" + materialId);
        inventory.setQuantity(bookStock);
        inventory.setAvailableQuantity(bookStock);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(bookStock.multiply(BigDecimal.TEN));
        inventory.setProductionDate(LocalDate.now());
        inventory.setExpireDate(LocalDate.now().plusYears(1));
        
        stockInventoryMapper.insert(inventory);
        
        // 模拟MaterialClient返回危化品列表
        when(materialClient.getHazardousMaterials())
                .thenReturn(Collections.singletonList(material));
        
        // 模拟ApprovalClient返回已领用未归还数量
        when(approvalClient.getUnreturnedQuantity(materialId))
                .thenReturn(unreturnedQuantity);
        
        // 2. 执行账实差异计算和预警检查
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // 3. 查询是否创建了预警记录
        LambdaQueryWrapper<AlertRecord> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.eq(AlertRecord::getBusinessType, "HAZARDOUS_MATERIAL")
                .eq(AlertRecord::getBusinessId, materialId)
                .eq(AlertRecord::getAlertType, 4);
        
        List<AlertRecord> alerts = alertRecordMapper.selectList(alertWrapper);
        
        // 4. 验证：差异略大于5%时，应创建预警
        assertThat(alerts)
                .as("当账实差异略大于5%%时，应创建预警记录")
                .isNotEmpty();
        
        AlertRecord alert = alerts.get(0);
        
        // 验证预警级别为严重（3）
        assertThat(alert.getAlertLevel())
                .as("预警级别应为严重（3）")
                .isEqualTo(3);
    }
    
    /**
     * 属性 16 边界情况测试：负差异（归还多了）
     * 
     * **Validates: Requirements 6.8**
     * 
     * 当账实差异为负数且绝对值>5%时，也应创建预警
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 16: 危化品账实差异预警")
    void hazardousDiscrepancyAlertWithNegativeDiscrepancy(
            @ForAll @Positive @BigRange(min = "100", max = "10000") BigDecimal bookStock,
            @ForAll @BigRange(min = "6", max = "50") BigDecimal discrepancyPercent) {
        
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis() + (long)(Math.random() * 1000);
        
        // 计算负差异的已领用未归还数量（归还多了）
        BigDecimal unreturnedQuantity = bookStock.multiply(discrepancyPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)).negate();
        
        // 创建危化品信息
        MaterialInfo material = new MaterialInfo();
        material.setId(materialId);
        material.setMaterialName("测试危化品-" + materialId);
        material.setMaterialType(3);
        material.setUnit("kg");
        material.setCasNumber("CAS-" + materialId);
        material.setDangerCategory("易燃液体");
        material.setIsControlled(1);
        
        // 创建库存记录
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(materialId);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH-" + materialId);
        inventory.setQuantity(bookStock);
        inventory.setAvailableQuantity(bookStock);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(bookStock.multiply(BigDecimal.TEN));
        inventory.setProductionDate(LocalDate.now());
        inventory.setExpireDate(LocalDate.now().plusYears(1));
        
        stockInventoryMapper.insert(inventory);
        
        // 模拟MaterialClient返回危化品列表
        when(materialClient.getHazardousMaterials())
                .thenReturn(Collections.singletonList(material));
        
        // 模拟ApprovalClient返回已领用未归还数量
        when(approvalClient.getUnreturnedQuantity(materialId))
                .thenReturn(unreturnedQuantity);
        
        // 2. 执行账实差异计算和预警检查
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // 3. 查询是否创建了预警记录
        LambdaQueryWrapper<AlertRecord> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.eq(AlertRecord::getBusinessType, "HAZARDOUS_MATERIAL")
                .eq(AlertRecord::getBusinessId, materialId)
                .eq(AlertRecord::getAlertType, 4);
        
        List<AlertRecord> alerts = alertRecordMapper.selectList(alertWrapper);
        
        // 4. 验证：负差异绝对值>5%时，应创建预警
        assertThat(alerts)
                .as("当账实差异为负数且绝对值>5%%时，应创建预警记录")
                .isNotEmpty();
        
        AlertRecord alert = alerts.get(0);
        
        // 验证预警级别为严重（3）
        assertThat(alert.getAlertLevel())
                .as("预警级别应为严重（3）")
                .isEqualTo(3);
        
        // 验证预警类型为账实差异（4）
        assertThat(alert.getAlertType())
                .as("预警类型应为账实差异（4）")
                .isEqualTo(4);
    }
    
    /**
     * 属性 16 边界情况测试：账面库存为0
     * 
     * **Validates: Requirements 6.8**
     * 
     * 当账面库存为0时，不应进行差异计算和预警
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 16: 危化品账实差异预警")
    void hazardousDiscrepancyAlertWithZeroBookStock() {
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis() + (long)(Math.random() * 1000);
        
        // 创建危化品信息
        MaterialInfo material = new MaterialInfo();
        material.setId(materialId);
        material.setMaterialName("测试危化品-" + materialId);
        material.setMaterialType(3);
        material.setUnit("kg");
        material.setCasNumber("CAS-" + materialId);
        material.setDangerCategory("易燃液体");
        material.setIsControlled(1);
        
        // 创建账面库存为0的库存记录
        StockInventory inventory = new StockInventory();
        inventory.setMaterialId(materialId);
        inventory.setWarehouseId(1L);
        inventory.setBatchNumber("BATCH-" + materialId);
        inventory.setQuantity(BigDecimal.ZERO);
        inventory.setAvailableQuantity(BigDecimal.ZERO);
        inventory.setLockedQuantity(BigDecimal.ZERO);
        inventory.setUnitPrice(BigDecimal.TEN);
        inventory.setTotalAmount(BigDecimal.ZERO);
        inventory.setProductionDate(LocalDate.now());
        inventory.setExpireDate(LocalDate.now().plusYears(1));
        
        stockInventoryMapper.insert(inventory);
        
        // 模拟MaterialClient返回危化品列表
        when(materialClient.getHazardousMaterials())
                .thenReturn(Collections.singletonList(material));
        
        // 2. 执行账实差异计算和预警检查
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // 3. 查询是否创建了预警记录
        LambdaQueryWrapper<AlertRecord> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.eq(AlertRecord::getBusinessType, "HAZARDOUS_MATERIAL")
                .eq(AlertRecord::getBusinessId, materialId)
                .eq(AlertRecord::getAlertType, 4);
        
        List<AlertRecord> alerts = alertRecordMapper.selectList(alertWrapper);
        
        // 4. 验证：账面库存为0时，不应创建预警
        assertThat(alerts)
                .as("当账面库存为0时，不应创建预警记录")
                .isEmpty();
        
        // 验证不应调用getUnreturnedQuantity
        verify(approvalClient, never()).getUnreturnedQuantity(materialId);
    }
}
