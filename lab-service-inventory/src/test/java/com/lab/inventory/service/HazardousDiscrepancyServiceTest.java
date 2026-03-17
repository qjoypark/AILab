package com.lab.inventory.service;

import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.entity.StockInventory;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.impl.HazardousDiscrepancyServiceImpl;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 危化品账实差异服务测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("危化品账实差异服务测试")
class HazardousDiscrepancyServiceTest {
    
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
    private List<StockInventory> inventories;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        hazardousMaterial = new MaterialInfo();
        hazardousMaterial.setId(1L);
        hazardousMaterial.setMaterialName("浓硫酸");
        hazardousMaterial.setMaterialType(3);
        hazardousMaterial.setIsControlled(0);
        hazardousMaterial.setUnit("瓶");
        
        inventories = new ArrayList<>();
        StockInventory inventory = new StockInventory();
        inventory.setId(1L);
        inventory.setMaterialId(1L);
        inventory.setQuantity(new BigDecimal("100"));
        inventories.add(inventory);
    }
    
    @Test
    @DisplayName("测试无危化品时不计算差异")
    void testCalculateDiscrepancy_NoHazardousMaterials() {
        // Given
        when(materialClient.getHazardousMaterials()).thenReturn(new ArrayList<>());
        
        // When
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then
        verify(materialClient, times(1)).getHazardousMaterials();
        verify(stockInventoryMapper, never()).selectList(any());
        verify(alertService, never()).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("测试账面库存为0时跳过计算")
    void testCalculateDiscrepancy_ZeroBookStock() {
        // Given
        List<MaterialInfo> materials = List.of(hazardousMaterial);
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        when(stockInventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        
        // When
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then
        verify(materialClient, times(1)).getHazardousMaterials();
        verify(stockInventoryMapper, times(1)).selectList(any());
        verify(approvalClient, never()).getUnreturnedQuantity(anyLong());
        verify(alertService, never()).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("测试差异小于5%时不触发预警")
    void testCalculateDiscrepancy_SmallDiscrepancy() {
        // Given
        List<MaterialInfo> materials = List.of(hazardousMaterial);
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        when(stockInventoryMapper.selectList(any())).thenReturn(inventories);
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("3")); // 3% 差异
        
        // When
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then
        verify(materialClient, times(1)).getHazardousMaterials();
        verify(stockInventoryMapper, times(1)).selectList(any());
        verify(approvalClient, times(1)).getUnreturnedQuantity(1L);
        verify(alertService, never()).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("测试差异超过5%时触发预警")
    void testCalculateDiscrepancy_LargeDiscrepancy() {
        // Given
        List<MaterialInfo> materials = List.of(hazardousMaterial);
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        when(stockInventoryMapper.selectList(any())).thenReturn(inventories);
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("10")); // 10% 差异
        when(alertService.createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString()))
            .thenReturn(1L);
        
        // When
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then
        verify(materialClient, times(1)).getHazardousMaterials();
        verify(stockInventoryMapper, times(1)).selectList(any());
        verify(approvalClient, times(1)).getUnreturnedQuantity(1L);
        verify(alertService, times(1)).createAlert(
            eq(4), // alertType: 4-账实差异
            eq(3), // alertLevel: 3-严重
            eq("HAZARDOUS_MATERIAL"),
            eq(1L),
            anyString(),
            anyString()
        );
    }
    
    @Test
    @DisplayName("测试多个危化品计算")
    void testCalculateDiscrepancy_MultipleMaterials() {
        // Given
        MaterialInfo material2 = new MaterialInfo();
        material2.setId(2L);
        material2.setMaterialName("盐酸");
        material2.setMaterialType(3);
        material2.setIsControlled(0);
        material2.setUnit("瓶");
        
        List<MaterialInfo> materials = List.of(hazardousMaterial, material2);
        
        StockInventory inventory2 = new StockInventory();
        inventory2.setId(2L);
        inventory2.setMaterialId(2L);
        inventory2.setQuantity(new BigDecimal("50"));
        
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        when(stockInventoryMapper.selectList(any()))
            .thenReturn(inventories)
            .thenReturn(List.of(inventory2));
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("10")); // 10% 差异
        when(approvalClient.getUnreturnedQuantity(2L)).thenReturn(new BigDecimal("2")); // 4% 差异
        when(alertService.createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString()))
            .thenReturn(1L);
        
        // When
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then
        verify(materialClient, times(1)).getHazardousMaterials();
        verify(stockInventoryMapper, times(2)).selectList(any());
        verify(approvalClient, times(1)).getUnreturnedQuantity(1L);
        verify(approvalClient, times(1)).getUnreturnedQuantity(2L);
        // 只有第一个材料触发预警
        verify(alertService, times(1)).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("测试计算过程中异常不影响其他材料")
    void testCalculateDiscrepancy_ExceptionHandling() {
        // Given
        MaterialInfo material2 = new MaterialInfo();
        material2.setId(2L);
        material2.setMaterialName("盐酸");
        material2.setMaterialType(3);
        material2.setIsControlled(0);
        material2.setUnit("瓶");
        
        List<MaterialInfo> materials = List.of(hazardousMaterial, material2);
        
        StockInventory inventory2 = new StockInventory();
        inventory2.setId(2L);
        inventory2.setMaterialId(2L);
        inventory2.setQuantity(new BigDecimal("50"));
        
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        when(stockInventoryMapper.selectList(any()))
            .thenThrow(new RuntimeException("Database error"))
            .thenReturn(List.of(inventory2));
        when(approvalClient.getUnreturnedQuantity(2L)).thenReturn(new BigDecimal("5")); // 10% 差异
        when(alertService.createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString()))
            .thenReturn(1L);
        
        // When
        hazardousDiscrepancyService.calculateDiscrepancy();
        
        // Then
        verify(materialClient, times(1)).getHazardousMaterials();
        verify(stockInventoryMapper, times(2)).selectList(any());
        verify(approvalClient, times(1)).getUnreturnedQuantity(2L);
        verify(alertService, times(1)).createAlert(anyInt(), anyInt(), anyString(), anyLong(), anyString(), anyString());
    }
}
