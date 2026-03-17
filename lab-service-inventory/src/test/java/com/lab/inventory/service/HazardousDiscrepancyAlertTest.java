package com.lab.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.entity.AlertRecord;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.AlertRecordMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.impl.HazardousDiscrepancyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 危化品账实差异预警测试
 * 
 * 测试任务 9.11: 实现危化品异常预警
 * - 验证当账实差异绝对值>5%时创建异常预警
 * - 验证预警级别设置为"严重"（level=3）
 * - 验证发送预警通知给中心管理员和安全管理员
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("危化品账实差异预警测试")
class HazardousDiscrepancyAlertTest {
    
    @Mock
    private MaterialClient materialClient;
    
    @Mock
    private ApprovalClient approvalClient;
    
    @Mock
    private StockInventoryMapper stockInventoryMapper;
    
    @Mock
    private AlertService alertService;
    
    @InjectMocks
    private HazardousDiscrepancyServiceImpl hazardousDiscrepancyService;
    
    private MaterialInfo hazardousMaterial;
    private StockInventory inventory;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        hazardousMaterial = new MaterialInfo();
        hazardousMaterial.setId(1L);
        hazardousMaterial.setMaterialName("浓硫酸");
        hazardousMaterial.setUnit("瓶");
        
        inventory = new StockInventory();
        inventory.setId(100L);
        inventory.setMaterialId(1L);
        inventory.setQuantity(new BigDecimal("100")); // 账面库存100瓶
    }
    
    @Test
    @DisplayName("当账实差异>5%时应创建严重级别预警")
    void shouldCreateSevereAlertWhenDiscrepancyExceeds5Percent() {
        // Given: 账面库存100瓶，已领用未归还10瓶，差异10%
        when(materialClient.getHazardousMaterials()).thenReturn(Collections.singletonList(hazardousMaterial));
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(inventory));
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("10"));
        when(alertService.createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(1L);
        
        // When: 执行账实差异计算
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then: 应创建预警
        ArgumentCaptor<Integer> alertTypeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> alertLevelCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> businessTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> businessIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(alertService).createAlert(
                alertTypeCaptor.capture(),
                alertLevelCaptor.capture(),
                businessTypeCaptor.capture(),
                businessIdCaptor.capture(),
                titleCaptor.capture(),
                contentCaptor.capture()
        );
        
        // 验证预警类型为账实差异（4）
        assertEquals(4, alertTypeCaptor.getValue(), "预警类型应为账实差异");
        
        // 验证预警级别为严重（3）
        assertEquals(3, alertLevelCaptor.getValue(), "预警级别应为严重");
        
        // 验证业务类型
        assertEquals("HAZARDOUS_MATERIAL", businessTypeCaptor.getValue(), "业务类型应为危化品");
        
        // 验证业务ID
        assertEquals(1L, businessIdCaptor.getValue(), "业务ID应为药品ID");
        
        // 验证预警标题
        assertEquals("危化品账实差异预警", titleCaptor.getValue(), "预警标题应正确");
        
        // 验证预警内容包含关键信息
        String content = contentCaptor.getValue();
        assertTrue(content.contains("浓硫酸"), "预警内容应包含药品名称");
        assertTrue(content.contains("100"), "预警内容应包含账面库存");
        assertTrue(content.contains("10"), "预警内容应包含已领用未归还数量");
        assertTrue(content.contains("90"), "预警内容应包含实际库存");
        assertTrue(content.contains("10.00%"), "预警内容应包含差异百分比");
    }
    
    @Test
    @DisplayName("当账实差异=5%时不应创建预警")
    void shouldNotCreateAlertWhenDiscrepancyEquals5Percent() {
        // Given: 账面库存100瓶，已领用未归还5瓶，差异5%
        when(materialClient.getHazardousMaterials()).thenReturn(Collections.singletonList(hazardousMaterial));
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(inventory));
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("5"));
        
        // When: 执行账实差异计算
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then: 不应创建预警
        verify(alertService, never()).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("当账实差异<5%时不应创建预警")
    void shouldNotCreateAlertWhenDiscrepancyLessThan5Percent() {
        // Given: 账面库存100瓶，已领用未归还3瓶，差异3%
        when(materialClient.getHazardousMaterials()).thenReturn(Collections.singletonList(hazardousMaterial));
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(inventory));
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("3"));
        
        // When: 执行账实差异计算
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then: 不应创建预警
        verify(alertService, never()).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("当账实差异为负数且绝对值>5%时应创建预警")
    void shouldCreateAlertWhenNegativeDiscrepancyExceeds5Percent() {
        // Given: 账面库存100瓶，已领用未归还-10瓶（归还多了），差异-10%
        when(materialClient.getHazardousMaterials()).thenReturn(Collections.singletonList(hazardousMaterial));
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(inventory));
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("-10"));
        when(alertService.createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(1L);
        
        // When: 执行账实差异计算
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then: 应创建预警
        verify(alertService).createAlert(eq(4), eq(3), eq("HAZARDOUS_MATERIAL"), eq(1L), anyString(), anyString());
    }
    
    @Test
    @DisplayName("当账面库存为0时不应计算差异")
    void shouldNotCalculateDiscrepancyWhenBookStockIsZero() {
        // Given: 账面库存为0
        inventory.setQuantity(BigDecimal.ZERO);
        when(materialClient.getHazardousMaterials()).thenReturn(Collections.singletonList(hazardousMaterial));
        when(stockInventoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(inventory));
        
        // When: 执行账实差异计算
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then: 不应创建预警
        verify(alertService, never()).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
        verify(approvalClient, never()).getUnreturnedQuantity(anyLong());
    }
    
    @Test
    @DisplayName("应处理多个危化品的账实差异")
    void shouldHandleMultipleHazardousMaterials() {
        // Given: 两个危化品，一个差异超标，一个正常
        MaterialInfo material1 = new MaterialInfo();
        material1.setId(1L);
        material1.setMaterialName("浓硫酸");
        material1.setUnit("瓶");
        
        MaterialInfo material2 = new MaterialInfo();
        material2.setId(2L);
        material2.setMaterialName("盐酸");
        material2.setUnit("瓶");
        
        StockInventory inventory1 = new StockInventory();
        inventory1.setId(100L);
        inventory1.setMaterialId(1L);
        inventory1.setQuantity(new BigDecimal("100"));
        
        StockInventory inventory2 = new StockInventory();
        inventory2.setId(101L);
        inventory2.setMaterialId(2L);
        inventory2.setQuantity(new BigDecimal("100"));
        
        when(materialClient.getHazardousMaterials()).thenReturn(Arrays.asList(material1, material2));
        when(stockInventoryMapper.selectList(argThat(wrapper -> {
            // 根据materialId返回不同的库存
            return true;
        }))).thenAnswer(invocation -> {
            LambdaQueryWrapper<StockInventory> wrapper = invocation.getArgument(0);
            // 简化处理，返回对应的库存
            return Arrays.asList(inventory1, inventory2);
        });
        
        // material1差异10%，material2差异3%
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("10"));
        when(approvalClient.getUnreturnedQuantity(2L)).thenReturn(new BigDecimal("3"));
        when(alertService.createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(1L);
        
        // When: 执行账实差异计算
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then: 只应为material1创建预警
        verify(alertService, times(1)).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("当没有危化品时不应执行任何操作")
    void shouldDoNothingWhenNoHazardousMaterials() {
        // Given: 没有危化品
        when(materialClient.getHazardousMaterials()).thenReturn(Collections.emptyList());
        
        // When: 执行账实差异计算
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then: 不应查询库存或创建预警
        verify(stockInventoryMapper, never()).selectList(any());
        verify(alertService, never()).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
    }
}
