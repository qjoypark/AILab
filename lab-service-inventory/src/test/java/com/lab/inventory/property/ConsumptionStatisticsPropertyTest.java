package com.lab.inventory.property;

import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.ConsumptionStatisticsDTO;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.entity.StockOut;
import com.lab.inventory.entity.StockOutDetail;
import com.lab.inventory.mapper.StockOutDetailMapper;
import com.lab.inventory.mapper.StockOutMapper;
import com.lab.inventory.service.ReportService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 消耗统计计算正确性属性测试
 * 
 * 使用jqwik框架进行基于属性的测试，每个属性测试运行100次迭代
 */
@SpringBootTest
@Transactional
public class ConsumptionStatisticsPropertyTest {
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private StockOutMapper stockOutMapper;
    
    @Autowired
    private StockOutDetailMapper stockOutDetailMapper;
    
    @MockBean
    private MaterialClient materialClient;
    
    /**
     * 属性 10: 消耗统计计算正确性
     * 
     * **Validates: Requirements 5.7**
     * 
     * 对于任何时间范围内的药品消耗统计，统计的消耗量应等于该时间范围内
     * 所有出库记录中该药品的出库数量之和，消耗成本应等于消耗量乘以对应的单价。
     * 
     * 验证要点：
     * 1. 总消耗量 = 所有物料消耗量之和
     * 2. 总成本 = 所有物料成本之和
     * 3. 成本占比总和 ≈ 100%（考虑舍入误差）
     * 4. 每个物料的成本 = 数量 × 单价
     * 5. 成本占比计算正确：(物料成本 / 总成本) × 100
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 10: 消耗统计计算正确性")
    void consumptionStatisticsCalculationCorrectness(
            @ForAll @IntRange(min = 1, max = 5) int materialCount,
            @ForAll @IntRange(min = 1, max = 3) int outOrdersPerMaterial) {
        
        // 1. 准备测试数据
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        List<Long> materialIds = new ArrayList<>();
        BigDecimal expectedTotalConsumption = BigDecimal.ZERO;
        BigDecimal expectedTotalCost = BigDecimal.ZERO;
        
        // 为每个物料创建多个出库单
        for (int i = 0; i < materialCount; i++) {
            Long materialId = System.currentTimeMillis() + i * 1000;
            materialIds.add(materialId);
            
            // 模拟物料信息
            MaterialInfo materialInfo = new MaterialInfo();
            materialInfo.setId(materialId);
            materialInfo.setMaterialName("测试物料-" + i);
            materialInfo.setMaterialCode("MAT-" + materialId);
            materialInfo.setSpecification("规格-" + i);
            materialInfo.setUnit("kg");
            materialInfo.setMaterialType(1); // 1-耗材
            
            when(materialClient.getMaterialInfo(materialId))
                    .thenReturn(materialInfo);
            
            // 为每个物料创建多个出库单
            for (int j = 0; j < outOrdersPerMaterial; j++) {
                // 创建出库单
                StockOut stockOut = new StockOut();
                stockOut.setOutOrderNo("OUT-" + materialId + "-" + j);
                stockOut.setOutType(1); // 1-领用出库
                stockOut.setWarehouseId(1L);
                stockOut.setOutDate(startDate.plusDays(j));
                stockOut.setOperatorId(1L);
                stockOut.setStatus(2); // 2-已出库
                stockOutMapper.insert(stockOut);
                
                // 创建出库明细
                BigDecimal quantity = new BigDecimal(10 + j * 5); // 变化的数量
                BigDecimal unitPrice = new BigDecimal(100 + i * 10); // 每个物料不同单价
                BigDecimal totalAmount = quantity.multiply(unitPrice);
                
                StockOutDetail detail = new StockOutDetail();
                detail.setOutOrderId(stockOut.getId());
                detail.setMaterialId(materialId);
                detail.setBatchNumber("BATCH-" + materialId + "-" + j);
                detail.setQuantity(quantity);
                detail.setUnitPrice(unitPrice);
                detail.setTotalAmount(totalAmount);
                stockOutDetailMapper.insert(detail);
                
                expectedTotalConsumption = expectedTotalConsumption.add(quantity);
                expectedTotalCost = expectedTotalCost.add(totalAmount);
            }
        }
        
        // 2. 执行消耗统计查询
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(
                startDate, endDate, null);
        
        // 3. 验证统计结果
        assertThat(result)
                .as("应返回消耗统计结果")
                .isNotNull();
        
        assertThat(result.getMaterials())
                .as("应返回物料消耗明细")
                .isNotNull()
                .hasSize(materialCount);
        
        // 3.1 验证总消耗量等于所有物料消耗量之和
        BigDecimal actualTotalConsumption = result.getMaterials().stream()
                .map(ConsumptionStatisticsDTO.MaterialConsumption::getConsumptionQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertThat(result.getTotalConsumption())
                .as("总消耗量应等于所有物料消耗量之和")
                .isEqualByComparingTo(actualTotalConsumption);
        
        assertThat(result.getTotalConsumption())
                .as("总消耗量应等于预期值")
                .isEqualByComparingTo(expectedTotalConsumption);
        
        // 3.2 验证总成本等于所有物料成本之和
        BigDecimal actualTotalCost = result.getMaterials().stream()
                .map(ConsumptionStatisticsDTO.MaterialConsumption::getConsumptionCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertThat(result.getTotalCost())
                .as("总成本应等于所有物料成本之和")
                .isEqualByComparingTo(actualTotalCost);
        
        assertThat(result.getTotalCost())
                .as("总成本应等于预期值")
                .isEqualByComparingTo(expectedTotalCost);
        
        // 3.3 验证成本占比总和约等于100%（考虑舍入误差）
        BigDecimal totalCostRate = result.getMaterials().stream()
                .map(ConsumptionStatisticsDTO.MaterialConsumption::getCostRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertThat(totalCostRate)
                .as("成本占比总和应约等于100%（允许±0.1%的舍入误差）")
                .isCloseTo(new BigDecimal("100"), within(new BigDecimal("0.1")));
        
        // 3.4 验证每个物料的成本计算正确性
        for (ConsumptionStatisticsDTO.MaterialConsumption material : result.getMaterials()) {
            assertThat(material.getMaterialId())
                    .as("物料ID不应为null")
                    .isNotNull();
            
            assertThat(material.getMaterialName())
                    .as("物料名称不应为null")
                    .isNotNull();
            
            assertThat(material.getConsumptionQuantity())
                    .as("消耗数量不应为null且应大于0")
                    .isNotNull()
                    .isGreaterThan(BigDecimal.ZERO);
            
            assertThat(material.getConsumptionCost())
                    .as("消耗成本不应为null且应大于0")
                    .isNotNull()
                    .isGreaterThan(BigDecimal.ZERO);
            
            assertThat(material.getCostRate())
                    .as("成本占比不应为null")
                    .isNotNull();
            
            // 验证成本占比计算正确：(物料成本 / 总成本) × 100
            BigDecimal expectedCostRate = material.getConsumptionCost()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(result.getTotalCost(), 2, RoundingMode.HALF_UP);
            
            assertThat(material.getCostRate())
                    .as("成本占比应等于：(物料成本 / 总成本) × 100")
                    .isEqualByComparingTo(expectedCostRate);
            
            // 验证成本占比在合理范围内
            assertThat(material.getCostRate().compareTo(BigDecimal.ZERO))
                    .as("成本占比应大于0")
                    .isGreaterThan(0);
            
            assertThat(material.getCostRate().compareTo(new BigDecimal("100")))
                    .as("成本占比应小于等于100%")
                    .isLessThanOrEqualTo(0);
        }
    }
    
    /**
     * 属性 10 边界情况测试：单个物料的消耗统计
     * 
     * **Validates: Requirements 5.7**
     * 
     * 当只有一个物料时，其成本占比应为100%
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 10: 消耗统计计算正确性")
    void singleMaterialConsumptionStatistics(
            @ForAll @Positive BigDecimal quantity,
            @ForAll @Positive BigDecimal unitPrice) {
        
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis();
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        // 模拟物料信息
        MaterialInfo materialInfo = new MaterialInfo();
        materialInfo.setId(materialId);
        materialInfo.setMaterialName("单一物料");
        materialInfo.setMaterialCode("MAT-SINGLE");
        materialInfo.setSpecification("规格A");
        materialInfo.setUnit("个");
        materialInfo.setMaterialType(1);
        
        when(materialClient.getMaterialInfo(materialId))
                .thenReturn(materialInfo);
        
        // 创建出库单
        StockOut stockOut = new StockOut();
        stockOut.setOutOrderNo("OUT-SINGLE-" + materialId);
        stockOut.setOutType(1);
        stockOut.setWarehouseId(1L);
        stockOut.setOutDate(startDate);
        stockOut.setOperatorId(1L);
        stockOut.setStatus(2);
        stockOutMapper.insert(stockOut);
        
        // 创建出库明细
        BigDecimal totalAmount = quantity.multiply(unitPrice);
        
        StockOutDetail detail = new StockOutDetail();
        detail.setOutOrderId(stockOut.getId());
        detail.setMaterialId(materialId);
        detail.setBatchNumber("BATCH-SINGLE");
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        detail.setTotalAmount(totalAmount);
        stockOutDetailMapper.insert(detail);
        
        // 2. 执行查询
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(
                startDate, endDate, null);
        
        // 3. 验证
        assertThat(result.getMaterials())
                .as("应返回1个物料")
                .hasSize(1);
        
        ConsumptionStatisticsDTO.MaterialConsumption material = result.getMaterials().get(0);
        
        assertThat(material.getConsumptionQuantity())
                .as("消耗数量应等于出库数量")
                .isEqualByComparingTo(quantity);
        
        assertThat(material.getConsumptionCost())
                .as("消耗成本应等于数量×单价")
                .isEqualByComparingTo(totalAmount);
        
        assertThat(material.getCostRate())
                .as("单一物料的成本占比应为100%")
                .isEqualByComparingTo(new BigDecimal("100"));
        
        assertThat(result.getTotalConsumption())
                .as("总消耗量应等于该物料消耗量")
                .isEqualByComparingTo(quantity);
        
        assertThat(result.getTotalCost())
                .as("总成本应等于该物料成本")
                .isEqualByComparingTo(totalAmount);
    }
    
    /**
     * 属性 10 边界情况测试：同一物料多次出库的累加
     * 
     * **Validates: Requirements 5.7**
     * 
     * 同一物料的多次出库应正确累加消耗量和成本
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 10: 消耗统计计算正确性")
    void multipleBatchesConsumptionAccumulation(
            @ForAll @IntRange(min = 2, max = 10) int batchCount) {
        
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis();
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        // 模拟物料信息
        MaterialInfo materialInfo = new MaterialInfo();
        materialInfo.setId(materialId);
        materialInfo.setMaterialName("多批次物料");
        materialInfo.setMaterialCode("MAT-MULTI");
        materialInfo.setSpecification("规格B");
        materialInfo.setUnit("箱");
        materialInfo.setMaterialType(2); // 2-试剂
        
        when(materialClient.getMaterialInfo(materialId))
                .thenReturn(materialInfo);
        
        BigDecimal expectedTotalQuantity = BigDecimal.ZERO;
        BigDecimal expectedTotalCost = BigDecimal.ZERO;
        BigDecimal unitPrice = new BigDecimal("50.00");
        
        // 创建多个出库单
        for (int i = 0; i < batchCount; i++) {
            StockOut stockOut = new StockOut();
            stockOut.setOutOrderNo("OUT-MULTI-" + materialId + "-" + i);
            stockOut.setOutType(1);
            stockOut.setWarehouseId(1L);
            stockOut.setOutDate(startDate.plusDays(i));
            stockOut.setOperatorId(1L);
            stockOut.setStatus(2);
            stockOutMapper.insert(stockOut);
            
            BigDecimal quantity = new BigDecimal(5 + i * 2);
            BigDecimal totalAmount = quantity.multiply(unitPrice);
            
            StockOutDetail detail = new StockOutDetail();
            detail.setOutOrderId(stockOut.getId());
            detail.setMaterialId(materialId);
            detail.setBatchNumber("BATCH-" + i);
            detail.setQuantity(quantity);
            detail.setUnitPrice(unitPrice);
            detail.setTotalAmount(totalAmount);
            stockOutDetailMapper.insert(detail);
            
            expectedTotalQuantity = expectedTotalQuantity.add(quantity);
            expectedTotalCost = expectedTotalCost.add(totalAmount);
        }
        
        // 2. 执行查询
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(
                startDate, endDate, null);
        
        // 3. 验证
        assertThat(result.getMaterials())
                .as("应返回1个物料（多批次合并）")
                .hasSize(1);
        
        ConsumptionStatisticsDTO.MaterialConsumption material = result.getMaterials().get(0);
        
        assertThat(material.getConsumptionQuantity())
                .as("消耗数量应等于所有批次数量之和")
                .isEqualByComparingTo(expectedTotalQuantity);
        
        assertThat(material.getConsumptionCost())
                .as("消耗成本应等于所有批次成本之和")
                .isEqualByComparingTo(expectedTotalCost);
        
        assertThat(material.getCostRate())
                .as("单一物料的成本占比应为100%")
                .isEqualByComparingTo(new BigDecimal("100"));
    }
    
    /**
     * 属性 10 边界情况测试：时间范围过滤
     * 
     * **Validates: Requirements 5.7**
     * 
     * 只统计指定时间范围内的出库记录
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 10: 消耗统计计算正确性")
    void dateRangeFiltering(
            @ForAll @IntRange(min = 1, max = 30) int daysInRange,
            @ForAll @IntRange(min = 1, max = 30) int daysOutOfRange) {
        
        // 1. 准备测试数据
        Long materialId = System.currentTimeMillis();
        LocalDate startDate = LocalDate.now().minusDays(daysInRange);
        LocalDate endDate = LocalDate.now();
        
        // 模拟物料信息
        MaterialInfo materialInfo = new MaterialInfo();
        materialInfo.setId(materialId);
        materialInfo.setMaterialName("时间范围测试物料");
        materialInfo.setMaterialCode("MAT-DATE");
        materialInfo.setSpecification("规格C");
        materialInfo.setUnit("L");
        materialInfo.setMaterialType(1);
        
        when(materialClient.getMaterialInfo(materialId))
                .thenReturn(materialInfo);
        
        BigDecimal unitPrice = new BigDecimal("100.00");
        BigDecimal quantityInRange = new BigDecimal("10.00");
        BigDecimal quantityOutOfRange = new BigDecimal("20.00");
        
        // 创建范围内的出库单
        StockOut stockOutIn = new StockOut();
        stockOutIn.setOutOrderNo("OUT-IN-" + materialId);
        stockOutIn.setOutType(1);
        stockOutIn.setWarehouseId(1L);
        stockOutIn.setOutDate(startDate.plusDays(1)); // 在范围内
        stockOutIn.setOperatorId(1L);
        stockOutIn.setStatus(2);
        stockOutMapper.insert(stockOutIn);
        
        StockOutDetail detailIn = new StockOutDetail();
        detailIn.setOutOrderId(stockOutIn.getId());
        detailIn.setMaterialId(materialId);
        detailIn.setBatchNumber("BATCH-IN");
        detailIn.setQuantity(quantityInRange);
        detailIn.setUnitPrice(unitPrice);
        detailIn.setTotalAmount(quantityInRange.multiply(unitPrice));
        stockOutDetailMapper.insert(detailIn);
        
        // 创建范围外的出库单（应被过滤）
        StockOut stockOutOut = new StockOut();
        stockOutOut.setOutOrderNo("OUT-OUT-" + materialId);
        stockOutOut.setOutType(1);
        stockOutOut.setWarehouseId(1L);
        stockOutOut.setOutDate(startDate.minusDays(daysOutOfRange)); // 在范围外
        stockOutOut.setOperatorId(1L);
        stockOutOut.setStatus(2);
        stockOutMapper.insert(stockOutOut);
        
        StockOutDetail detailOut = new StockOutDetail();
        detailOut.setOutOrderId(stockOutOut.getId());
        detailOut.setMaterialId(materialId);
        detailOut.setBatchNumber("BATCH-OUT");
        detailOut.setQuantity(quantityOutOfRange);
        detailOut.setUnitPrice(unitPrice);
        detailOut.setTotalAmount(quantityOutOfRange.multiply(unitPrice));
        stockOutDetailMapper.insert(detailOut);
        
        // 2. 执行查询（指定时间范围）
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(
                startDate, endDate, null);
        
        // 3. 验证
        assertThat(result.getMaterials())
                .as("应返回1个物料")
                .hasSize(1);
        
        ConsumptionStatisticsDTO.MaterialConsumption material = result.getMaterials().get(0);
        
        assertThat(material.getConsumptionQuantity())
                .as("只应统计范围内的消耗量")
                .isEqualByComparingTo(quantityInRange);
        
        assertThat(material.getConsumptionCost())
                .as("只应统计范围内的成本")
                .isEqualByComparingTo(quantityInRange.multiply(unitPrice));
        
        assertThat(result.getTotalConsumption())
                .as("总消耗量应只包含范围内的数据")
                .isEqualByComparingTo(quantityInRange);
        
        assertThat(result.getTotalCost())
                .as("总成本应只包含范围内的数据")
                .isEqualByComparingTo(quantityInRange.multiply(unitPrice));
    }
    
    /**
     * 属性 10 边界情况测试：物料类型过滤
     * 
     * **Validates: Requirements 5.7**
     * 
     * 当指定物料类型时，只统计该类型的物料
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 10: 消耗统计计算正确性")
    void materialTypeFiltering(
            @ForAll @IntRange(min = 1, max = 3) int targetType) {
        
        // 1. 准备测试数据
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        List<Long> materialIds = new ArrayList<>();
        BigDecimal expectedQuantity = BigDecimal.ZERO;
        BigDecimal expectedCost = BigDecimal.ZERO;
        
        // 创建不同类型的物料
        for (int type = 1; type <= 3; type++) {
            Long materialId = System.currentTimeMillis() + type * 1000;
            materialIds.add(materialId);
            
            MaterialInfo materialInfo = new MaterialInfo();
            materialInfo.setId(materialId);
            materialInfo.setMaterialName("物料类型-" + type);
            materialInfo.setMaterialCode("MAT-TYPE-" + type);
            materialInfo.setSpecification("规格");
            materialInfo.setUnit("个");
            materialInfo.setMaterialType(type); // 1-耗材, 2-试剂, 3-危化品
            
            when(materialClient.getMaterialInfo(materialId))
                    .thenReturn(materialInfo);
            
            // 创建出库单
            StockOut stockOut = new StockOut();
            stockOut.setOutOrderNo("OUT-TYPE-" + type + "-" + materialId);
            stockOut.setOutType(1);
            stockOut.setWarehouseId(1L);
            stockOut.setOutDate(startDate);
            stockOut.setOperatorId(1L);
            stockOut.setStatus(2);
            stockOutMapper.insert(stockOut);
            
            BigDecimal quantity = new BigDecimal(10 * type);
            BigDecimal unitPrice = new BigDecimal(100);
            BigDecimal totalAmount = quantity.multiply(unitPrice);
            
            StockOutDetail detail = new StockOutDetail();
            detail.setOutOrderId(stockOut.getId());
            detail.setMaterialId(materialId);
            detail.setBatchNumber("BATCH-TYPE-" + type);
            detail.setQuantity(quantity);
            detail.setUnitPrice(unitPrice);
            detail.setTotalAmount(totalAmount);
            stockOutDetailMapper.insert(detail);
            
            // 只累加目标类型的数据
            if (type == targetType) {
                expectedQuantity = expectedQuantity.add(quantity);
                expectedCost = expectedCost.add(totalAmount);
            }
        }
        
        // 2. 执行查询（指定物料类型）
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(
                startDate, endDate, targetType);
        
        // 3. 验证
        assertThat(result.getMaterials())
                .as("应只返回指定类型的物料")
                .hasSize(1);
        
        ConsumptionStatisticsDTO.MaterialConsumption material = result.getMaterials().get(0);
        
        assertThat(material.getConsumptionQuantity())
                .as("消耗数量应只包含指定类型的物料")
                .isEqualByComparingTo(expectedQuantity);
        
        assertThat(material.getConsumptionCost())
                .as("消耗成本应只包含指定类型的物料")
                .isEqualByComparingTo(expectedCost);
        
        assertThat(result.getTotalConsumption())
                .as("总消耗量应只包含指定类型的物料")
                .isEqualByComparingTo(expectedQuantity);
        
        assertThat(result.getTotalCost())
                .as("总成本应只包含指定类型的物料")
                .isEqualByComparingTo(expectedCost);
    }
    
    /**
     * 属性 10 边界情况测试：空结果处理
     * 
     * **Validates: Requirements 5.7**
     * 
     * 当没有出库记录时，应返回空的统计结果
     */
    @Property(tries = 100)
    @Tag("Feature: smart-lab-management-system, Property 10: 消耗统计计算正确性")
    void emptyConsumptionStatistics() {
        
        // 1. 执行查询（时间范围内没有出库记录）
        LocalDate startDate = LocalDate.now().plusYears(1); // 未来日期
        LocalDate endDate = LocalDate.now().plusYears(2);
        
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(
                startDate, endDate, null);
        
        // 2. 验证
        assertThat(result)
                .as("应返回结果对象")
                .isNotNull();
        
        assertThat(result.getTotalConsumption())
                .as("总消耗量应为0")
                .isEqualByComparingTo(BigDecimal.ZERO);
        
        assertThat(result.getTotalCost())
                .as("总成本应为0")
                .isEqualByComparingTo(BigDecimal.ZERO);
        
        assertThat(result.getMaterials())
                .as("物料列表应为空")
                .isNotNull()
                .isEmpty();
    }
}
