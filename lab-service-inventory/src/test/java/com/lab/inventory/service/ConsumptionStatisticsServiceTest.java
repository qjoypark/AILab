package com.lab.inventory.service;

import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.ConsumptionStatisticsDTO;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.entity.StockOut;
import com.lab.inventory.entity.StockOutDetail;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.mapper.StockOutDetailMapper;
import com.lab.inventory.mapper.StockOutMapper;
import com.lab.inventory.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 消耗统计服务测试
 * 
 * **验证需求: 5.7, 10.2**
 */
@ExtendWith(MockitoExtension.class)
class ConsumptionStatisticsServiceTest {
    
    @Mock
    private StockInventoryMapper stockInventoryMapper;
    
    @Mock
    private StockOutMapper stockOutMapper;
    
    @Mock
    private StockOutDetailMapper stockOutDetailMapper;
    
    @Mock
    private MaterialClient materialClient;
    
    @InjectMocks
    private ReportServiceImpl reportService;
    
    private LocalDate startDate;
    private LocalDate endDate;
    
    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);
    }
    
    @Test
    void testGetConsumptionStatistics_WithValidData() {
        // Given: 准备出库单数据
        StockOut stockOut1 = createStockOut(1L, LocalDate.of(2024, 1, 15));
        StockOut stockOut2 = createStockOut(2L, LocalDate.of(2024, 1, 20));
        when(stockOutMapper.selectList(any())).thenReturn(Arrays.asList(stockOut1, stockOut2));
        
        // 准备出库明细数据
        StockOutDetail detail1 = createStockOutDetail(1L, 1L, 100L, new BigDecimal("10.00"), new BigDecimal("5.00"), new BigDecimal("50.00"));
        StockOutDetail detail2 = createStockOutDetail(2L, 1L, 101L, new BigDecimal("20.00"), new BigDecimal("3.00"), new BigDecimal("60.00"));
        StockOutDetail detail3 = createStockOutDetail(3L, 2L, 100L, new BigDecimal("15.00"), new BigDecimal("5.00"), new BigDecimal("75.00"));
        when(stockOutDetailMapper.selectList(any())).thenReturn(Arrays.asList(detail1, detail2, detail3));
        
        // 准备物料信息
        MaterialInfo material100 = createMaterialInfo(100L, "试剂A", "REG-001", "500ml", "瓶", 2);
        MaterialInfo material101 = createMaterialInfo(101L, "试剂B", "REG-002", "1L", "瓶", 2);
        when(materialClient.getMaterialInfo(100L)).thenReturn(material100);
        when(materialClient.getMaterialInfo(101L)).thenReturn(material101);
        
        // When: 查询消耗统计
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(startDate, endDate, null);
        
        // Then: 验证结果
        assertNotNull(result);
        assertEquals(new BigDecimal("45.00"), result.getTotalConsumption()); // 10+20+15
        assertEquals(new BigDecimal("185.00"), result.getTotalCost()); // 50+60+75
        assertEquals(2, result.getMaterials().size());
        
        // 验证物料A（试剂A）的统计
        ConsumptionStatisticsDTO.MaterialConsumption materialA = result.getMaterials().stream()
            .filter(m -> m.getMaterialId().equals(100L))
            .findFirst()
            .orElse(null);
        assertNotNull(materialA);
        assertEquals("试剂A", materialA.getMaterialName());
        assertEquals(new BigDecimal("25.00"), materialA.getConsumptionQuantity()); // 10+15
        assertEquals(new BigDecimal("125.00"), materialA.getConsumptionCost()); // 50+75
        assertEquals(new BigDecimal("67.57"), materialA.getCostRate()); // 125/185*100
        
        // 验证物料B（试剂B）的统计
        ConsumptionStatisticsDTO.MaterialConsumption materialB = result.getMaterials().stream()
            .filter(m -> m.getMaterialId().equals(101L))
            .findFirst()
            .orElse(null);
        assertNotNull(materialB);
        assertEquals("试剂B", materialB.getMaterialName());
        assertEquals(new BigDecimal("20.00"), materialB.getConsumptionQuantity());
        assertEquals(new BigDecimal("60.00"), materialB.getConsumptionCost());
        assertEquals(new BigDecimal("32.43"), materialB.getCostRate()); // 60/185*100
    }
    
    @Test
    void testGetConsumptionStatistics_FilterByMaterialType() {
        // Given: 准备混合类型的物料数据
        StockOut stockOut = createStockOut(1L, LocalDate.of(2024, 1, 15));
        when(stockOutMapper.selectList(any())).thenReturn(Collections.singletonList(stockOut));
        
        StockOutDetail detail1 = createStockOutDetail(1L, 1L, 100L, new BigDecimal("10.00"), new BigDecimal("5.00"), new BigDecimal("50.00"));
        StockOutDetail detail2 = createStockOutDetail(2L, 1L, 101L, new BigDecimal("20.00"), new BigDecimal("3.00"), new BigDecimal("60.00"));
        when(stockOutDetailMapper.selectList(any())).thenReturn(Arrays.asList(detail1, detail2));
        
        // 物料100是试剂（类型2），物料101是耗材（类型1）
        MaterialInfo material100 = createMaterialInfo(100L, "试剂A", "REG-001", "500ml", "瓶", 2);
        MaterialInfo material101 = createMaterialInfo(101L, "耗材A", "CON-001", "盒", "盒", 1);
        when(materialClient.getMaterialInfo(100L)).thenReturn(material100);
        when(materialClient.getMaterialInfo(101L)).thenReturn(material101);
        
        // When: 只查询试剂类型（类型2）
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(startDate, endDate, 2);
        
        // Then: 只返回试剂的统计
        assertNotNull(result);
        assertEquals(1, result.getMaterials().size());
        assertEquals(100L, result.getMaterials().get(0).getMaterialId());
        assertEquals("试剂A", result.getMaterials().get(0).getMaterialName());
        assertEquals(new BigDecimal("10.00"), result.getTotalConsumption());
        assertEquals(new BigDecimal("50.00"), result.getTotalCost());
    }
    
    @Test
    void testGetConsumptionStatistics_EmptyResult_NoStockOut() {
        // Given: 没有出库记录
        when(stockOutMapper.selectList(any())).thenReturn(Collections.emptyList());
        
        // When: 查询消耗统计
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(startDate, endDate, null);
        
        // Then: 返回空报表
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalConsumption());
        assertEquals(BigDecimal.ZERO, result.getTotalCost());
        assertTrue(result.getMaterials().isEmpty());
    }
    
    @Test
    void testGetConsumptionStatistics_EmptyResult_NoDetails() {
        // Given: 有出库单但没有明细
        StockOut stockOut = createStockOut(1L, LocalDate.of(2024, 1, 15));
        when(stockOutMapper.selectList(any())).thenReturn(Collections.singletonList(stockOut));
        when(stockOutDetailMapper.selectList(any())).thenReturn(Collections.emptyList());
        
        // When: 查询消耗统计
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(startDate, endDate, null);
        
        // Then: 返回空报表
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalConsumption());
        assertEquals(BigDecimal.ZERO, result.getTotalCost());
        assertTrue(result.getMaterials().isEmpty());
    }
    
    @Test
    void testGetConsumptionStatistics_SortedByCostDescending() {
        // Given: 准备多个物料，成本不同
        StockOut stockOut = createStockOut(1L, LocalDate.of(2024, 1, 15));
        when(stockOutMapper.selectList(any())).thenReturn(Collections.singletonList(stockOut));
        
        StockOutDetail detail1 = createStockOutDetail(1L, 1L, 100L, new BigDecimal("10.00"), new BigDecimal("5.00"), new BigDecimal("50.00"));
        StockOutDetail detail2 = createStockOutDetail(2L, 1L, 101L, new BigDecimal("20.00"), new BigDecimal("10.00"), new BigDecimal("200.00"));
        StockOutDetail detail3 = createStockOutDetail(3L, 1L, 102L, new BigDecimal("15.00"), new BigDecimal("8.00"), new BigDecimal("120.00"));
        when(stockOutDetailMapper.selectList(any())).thenReturn(Arrays.asList(detail1, detail2, detail3));
        
        MaterialInfo material100 = createMaterialInfo(100L, "物料A", "MAT-001", "个", "个", 1);
        MaterialInfo material101 = createMaterialInfo(101L, "物料B", "MAT-002", "个", "个", 1);
        MaterialInfo material102 = createMaterialInfo(102L, "物料C", "MAT-003", "个", "个", 1);
        when(materialClient.getMaterialInfo(100L)).thenReturn(material100);
        when(materialClient.getMaterialInfo(101L)).thenReturn(material101);
        when(materialClient.getMaterialInfo(102L)).thenReturn(material102);
        
        // When: 查询消耗统计
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(startDate, endDate, null);
        
        // Then: 验证按成本降序排序
        assertNotNull(result);
        assertEquals(3, result.getMaterials().size());
        assertEquals(101L, result.getMaterials().get(0).getMaterialId()); // 成本200，最高
        assertEquals(102L, result.getMaterials().get(1).getMaterialId()); // 成本120，第二
        assertEquals(100L, result.getMaterials().get(2).getMaterialId()); // 成本50，最低
    }
    
    @Test
    void testGetConsumptionStatistics_CostRateCalculation() {
        // Given: 准备数据验证成本占比计算
        StockOut stockOut = createStockOut(1L, LocalDate.of(2024, 1, 15));
        when(stockOutMapper.selectList(any())).thenReturn(Collections.singletonList(stockOut));
        
        StockOutDetail detail1 = createStockOutDetail(1L, 1L, 100L, new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("100.00"));
        StockOutDetail detail2 = createStockOutDetail(2L, 1L, 101L, new BigDecimal("20.00"), new BigDecimal("15.00"), new BigDecimal("300.00"));
        when(stockOutDetailMapper.selectList(any())).thenReturn(Arrays.asList(detail1, detail2));
        
        MaterialInfo material100 = createMaterialInfo(100L, "物料A", "MAT-001", "个", "个", 1);
        MaterialInfo material101 = createMaterialInfo(101L, "物料B", "MAT-002", "个", "个", 1);
        when(materialClient.getMaterialInfo(100L)).thenReturn(material100);
        when(materialClient.getMaterialInfo(101L)).thenReturn(material101);
        
        // When: 查询消耗统计
        ConsumptionStatisticsDTO result = reportService.getConsumptionStatistics(startDate, endDate, null);
        
        // Then: 验证成本占比
        assertNotNull(result);
        assertEquals(new BigDecimal("400.00"), result.getTotalCost());
        
        ConsumptionStatisticsDTO.MaterialConsumption materialA = result.getMaterials().stream()
            .filter(m -> m.getMaterialId().equals(100L))
            .findFirst()
            .orElse(null);
        assertNotNull(materialA);
        assertEquals(new BigDecimal("25.00"), materialA.getCostRate()); // 100/400*100
        
        ConsumptionStatisticsDTO.MaterialConsumption materialB = result.getMaterials().stream()
            .filter(m -> m.getMaterialId().equals(101L))
            .findFirst()
            .orElse(null);
        assertNotNull(materialB);
        assertEquals(new BigDecimal("75.00"), materialB.getCostRate()); // 300/400*100
    }
    
    // Helper methods
    
    private StockOut createStockOut(Long id, LocalDate outDate) {
        StockOut stockOut = new StockOut();
        stockOut.setId(id);
        stockOut.setOutOrderNo("OUT-" + id);
        stockOut.setOutType(1);
        stockOut.setWarehouseId(1L);
        stockOut.setOutDate(outDate);
        stockOut.setStatus(2); // 已出库
        return stockOut;
    }
    
    private StockOutDetail createStockOutDetail(Long id, Long outOrderId, Long materialId, 
                                                BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalAmount) {
        StockOutDetail detail = new StockOutDetail();
        detail.setId(id);
        detail.setOutOrderId(outOrderId);
        detail.setMaterialId(materialId);
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        detail.setTotalAmount(totalAmount);
        return detail;
    }
    
    private MaterialInfo createMaterialInfo(Long id, String name, String code, 
                                           String specification, String unit, Integer materialType) {
        MaterialInfo info = new MaterialInfo();
        info.setId(id);
        info.setMaterialName(name);
        info.setMaterialCode(code);
        info.setSpecification(specification);
        info.setUnit(unit);
        info.setMaterialType(materialType);
        info.setCategoryId(1L);
        return info;
    }
}
