package com.lab.inventory.service;

import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.MaterialCategoryInfo;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.dto.StockSummaryDTO;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 报表服务测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("报表服务测试")
class ReportServiceTest {
    
    @Mock
    private StockInventoryMapper stockInventoryMapper;
    
    @Mock
    private MaterialClient materialClient;
    
    @InjectMocks
    private ReportServiceImpl reportService;
    
    private List<StockInventory> mockInventories;
    private MaterialInfo material1;
    private MaterialInfo material2;
    private MaterialCategoryInfo category1;
    private MaterialCategoryInfo category2;
    
    @BeforeEach
    void setUp() {
        // 准备分类数据
        category1 = new MaterialCategoryInfo();
        category1.setId(1L);
        category1.setCategoryName("化学试剂");
        
        category2 = new MaterialCategoryInfo();
        category2.setId(2L);
        category2.setCategoryName("实验耗材");
        
        // 准备物料数据
        material1 = new MaterialInfo();
        material1.setId(1L);
        material1.setMaterialName("盐酸");
        material1.setMaterialType(2); // 试剂
        material1.setCategoryId(1L);
        
        material2 = new MaterialInfo();
        material2.setId(2L);
        material2.setMaterialName("试管");
        material2.setMaterialType(1); // 耗材
        material2.setCategoryId(2L);
        
        // 准备库存数据
        mockInventories = new ArrayList<>();
        
        StockInventory stock1 = new StockInventory();
        stock1.setId(1L);
        stock1.setMaterialId(1L);
        stock1.setWarehouseId(1L);
        stock1.setQuantity(new BigDecimal("100"));
        stock1.setUnitPrice(new BigDecimal("50"));
        mockInventories.add(stock1);
        
        StockInventory stock2 = new StockInventory();
        stock2.setId(2L);
        stock2.setMaterialId(2L);
        stock2.setWarehouseId(1L);
        stock2.setQuantity(new BigDecimal("200"));
        stock2.setUnitPrice(new BigDecimal("10"));
        mockInventories.add(stock2);
    }
    
    @Test
    @DisplayName("应该正确生成库存汇总报表")
    void shouldGenerateStockSummaryCorrectly() {
        // Given
        when(stockInventoryMapper.selectList(any())).thenReturn(mockInventories);
        when(materialClient.getMaterialInfo(1L)).thenReturn(material1);
        when(materialClient.getMaterialInfo(2L)).thenReturn(material2);
        when(materialClient.getCategoryInfo(1L)).thenReturn(category1);
        when(materialClient.getCategoryInfo(2L)).thenReturn(category2);
        
        // When
        StockSummaryDTO result = reportService.getStockSummary(null, null);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalValue()).isEqualByComparingTo(new BigDecimal("7000")); // 100*50 + 200*10
        assertThat(result.getCategories()).hasSize(2);
        
        // 验证分类1（化学试剂）
        StockSummaryDTO.CategorySummary cat1 = result.getCategories().stream()
            .filter(c -> c.getCategoryId().equals(1L))
            .findFirst()
            .orElse(null);
        assertThat(cat1).isNotNull();
        assertThat(cat1.getCategoryName()).isEqualTo("化学试剂");
        assertThat(cat1.getItemCount()).isEqualTo(1);
        assertThat(cat1.getTotalQuantity()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(cat1.getTotalValue()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(cat1.getValuePercentage()).isEqualByComparingTo(new BigDecimal("71.43"));
        
        // 验证分类2（实验耗材）
        StockSummaryDTO.CategorySummary cat2 = result.getCategories().stream()
            .filter(c -> c.getCategoryId().equals(2L))
            .findFirst()
            .orElse(null);
        assertThat(cat2).isNotNull();
        assertThat(cat2.getCategoryName()).isEqualTo("实验耗材");
        assertThat(cat2.getItemCount()).isEqualTo(1);
        assertThat(cat2.getTotalQuantity()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(cat2.getTotalValue()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(cat2.getValuePercentage()).isEqualByComparingTo(new BigDecimal("28.57"));
    }
    
    @Test
    @DisplayName("应该按物料类型过滤库存")
    void shouldFilterByMaterialType() {
        // Given
        when(stockInventoryMapper.selectList(any())).thenReturn(mockInventories);
        when(materialClient.getMaterialInfo(1L)).thenReturn(material1);
        when(materialClient.getMaterialInfo(2L)).thenReturn(material2);
        when(materialClient.getCategoryInfo(1L)).thenReturn(category1);
        
        // When - 只查询试剂类型
        StockSummaryDTO result = reportService.getStockSummary(null, 2);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalValue()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getCategories().get(0).getCategoryName()).isEqualTo("化学试剂");
    }
    
    @Test
    @DisplayName("应该处理空库存情况")
    void shouldHandleEmptyInventory() {
        // Given
        when(stockInventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        
        // When
        StockSummaryDTO result = reportService.getStockSummary(null, null);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getCategories()).isEmpty();
    }
    
    @Test
    @DisplayName("应该正确处理同一分类下的多个物料")
    void shouldHandleMultipleMaterialsInSameCategory() {
        // Given
        MaterialInfo material3 = new MaterialInfo();
        material3.setId(3L);
        material3.setMaterialName("硫酸");
        material3.setMaterialType(2);
        material3.setCategoryId(1L); // 同属化学试剂分类
        
        StockInventory stock3 = new StockInventory();
        stock3.setId(3L);
        stock3.setMaterialId(3L);
        stock3.setWarehouseId(1L);
        stock3.setQuantity(new BigDecimal("50"));
        stock3.setUnitPrice(new BigDecimal("60"));
        mockInventories.add(stock3);
        
        when(stockInventoryMapper.selectList(any())).thenReturn(mockInventories);
        when(materialClient.getMaterialInfo(1L)).thenReturn(material1);
        when(materialClient.getMaterialInfo(2L)).thenReturn(material2);
        when(materialClient.getMaterialInfo(3L)).thenReturn(material3);
        when(materialClient.getCategoryInfo(1L)).thenReturn(category1);
        when(materialClient.getCategoryInfo(2L)).thenReturn(category2);
        
        // When
        StockSummaryDTO result = reportService.getStockSummary(null, null);
        
        // Then
        StockSummaryDTO.CategorySummary cat1 = result.getCategories().stream()
            .filter(c -> c.getCategoryId().equals(1L))
            .findFirst()
            .orElse(null);
        
        assertThat(cat1).isNotNull();
        assertThat(cat1.getItemCount()).isEqualTo(2); // 两个不同的物料
        assertThat(cat1.getTotalValue()).isEqualByComparingTo(new BigDecimal("8000")); // 5000 + 3000
    }
}
