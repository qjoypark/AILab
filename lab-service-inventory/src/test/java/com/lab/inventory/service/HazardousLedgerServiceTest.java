package com.lab.inventory.service;

import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.client.MaterialClient;
import com.lab.inventory.dto.HazardousLedgerDTO;
import com.lab.inventory.dto.HazardousLedgerQueryDTO;
import com.lab.inventory.dto.MaterialInfo;
import com.lab.inventory.entity.StockInDetail;
import com.lab.inventory.entity.StockOutDetail;
import com.lab.inventory.mapper.StockInDetailMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.mapper.StockOutDetailMapper;
import com.lab.inventory.service.impl.HazardousLedgerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 危化品台账服务测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("危化品台账服务测试")
class HazardousLedgerServiceTest {
    
    @Mock
    private MaterialClient materialClient;
    
    @Mock
    private ApprovalClient approvalClient;
    
    @Mock
    private StockInventoryMapper stockInventoryMapper;
    
    @Mock
    private StockInDetailMapper stockInDetailMapper;
    
    @Mock
    private StockOutDetailMapper stockOutDetailMapper;
    
    @InjectMocks
    private HazardousLedgerServiceImpl hazardousLedgerService;
    
    private MaterialInfo hazardousMaterial;
    
    @BeforeEach
    void setUp() {
        hazardousMaterial = new MaterialInfo();
        hazardousMaterial.setId(1L);
        hazardousMaterial.setMaterialName("盐酸");
        hazardousMaterial.setMaterialType(3);
        hazardousMaterial.setIsControlled(0);
        hazardousMaterial.setUnit("L");
        hazardousMaterial.setCasNumber("7647-01-0");
        hazardousMaterial.setDangerCategory("腐蚀品");
    }
    
    @Test
    @DisplayName("查询危化品台账 - 正常情况")
    void testQueryLedger_Normal() {
        // Given
        List<MaterialInfo> materials = List.of(hazardousMaterial);
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        
        // 模拟入库明细
        List<StockInDetail> inDetails = new ArrayList<>();
        StockInDetail inDetail = new StockInDetail();
        inDetail.setMaterialId(1L);
        inDetail.setQuantity(new BigDecimal("100"));
        inDetails.add(inDetail);
        when(stockInDetailMapper.selectList(any())).thenReturn(inDetails);
        
        // 模拟出库明细
        List<StockOutDetail> outDetails = new ArrayList<>();
        StockOutDetail outDetail = new StockOutDetail();
        outDetail.setMaterialId(1L);
        outDetail.setQuantity(new BigDecimal("30"));
        outDetails.add(outDetail);
        when(stockOutDetailMapper.selectList(any())).thenReturn(outDetails);
        
        // 模拟已领用未归还数量
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("10"));
        
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        queryDTO.setStartDate(LocalDate.of(2024, 1, 1));
        queryDTO.setEndDate(LocalDate.of(2024, 12, 31));
        
        // When
        List<HazardousLedgerDTO> result = hazardousLedgerService.queryLedger(queryDTO);
        
        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        
        HazardousLedgerDTO ledger = result.get(0);
        assertThat(ledger.getMaterialId()).isEqualTo(1L);
        assertThat(ledger.getMaterialName()).isEqualTo("盐酸");
        assertThat(ledger.getCasNumber()).isEqualTo("7647-01-0");
        assertThat(ledger.getDangerCategory()).isEqualTo("腐蚀品");
        assertThat(ledger.getControlType()).isEqualTo(0);
        assertThat(ledger.getUnit()).isEqualTo("L");
        assertThat(ledger.getTotalStockIn()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(ledger.getTotalStockOut()).isEqualByComparingTo(new BigDecimal("30"));
    }
    
    @Test
    @DisplayName("查询危化品台账 - 指定药品ID")
    void testQueryLedger_WithMaterialId() {
        // Given
        MaterialInfo material1 = new MaterialInfo();
        material1.setId(1L);
        material1.setMaterialName("盐酸");
        material1.setMaterialType(3);
        material1.setIsControlled(0);
        material1.setUnit("L");
        
        MaterialInfo material2 = new MaterialInfo();
        material2.setId(2L);
        material2.setMaterialName("硫酸");
        material2.setMaterialType(3);
        material2.setIsControlled(0);
        material2.setUnit("L");
        
        List<MaterialInfo> materials = List.of(material1, material2);
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        
        when(stockInDetailMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(stockOutDetailMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(approvalClient.getUnreturnedQuantity(any())).thenReturn(BigDecimal.ZERO);
        
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        queryDTO.setMaterialId(1L);
        
        // When
        List<HazardousLedgerDTO> result = hazardousLedgerService.queryLedger(queryDTO);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaterialId()).isEqualTo(1L);
        assertThat(result.get(0).getMaterialName()).isEqualTo("盐酸");
    }
    
    @Test
    @DisplayName("查询危化品台账 - 无危化品")
    void testQueryLedger_NoHazardousMaterials() {
        // Given
        when(materialClient.getHazardousMaterials()).thenReturn(new ArrayList<>());
        
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        
        // When
        List<HazardousLedgerDTO> result = hazardousLedgerService.queryLedger(queryDTO);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("查询危化品台账 - 计算账实差异")
    void testQueryLedger_CalculateDiscrepancy() {
        // Given
        List<MaterialInfo> materials = List.of(hazardousMaterial);
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        
        // 模拟入库100L
        List<StockInDetail> inDetails = new ArrayList<>();
        StockInDetail inDetail = new StockInDetail();
        inDetail.setMaterialId(1L);
        inDetail.setQuantity(new BigDecimal("100"));
        inDetails.add(inDetail);
        when(stockInDetailMapper.selectList(any())).thenReturn(inDetails);
        
        // 模拟出库30L
        List<StockOutDetail> outDetails = new ArrayList<>();
        StockOutDetail outDetail = new StockOutDetail();
        outDetail.setMaterialId(1L);
        outDetail.setQuantity(new BigDecimal("30"));
        outDetails.add(outDetail);
        when(stockOutDetailMapper.selectList(any())).thenReturn(outDetails);
        
        // 模拟已领用未归还10L
        // 期末库存 = 100 - 30 = 70L
        // 实际库存 = 70 - 10 = 60L
        // 账实差异 = (70 - 60) / 70 * 100% = 14.29%
        when(approvalClient.getUnreturnedQuantity(1L)).thenReturn(new BigDecimal("10"));
        
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        
        // When
        List<HazardousLedgerDTO> result = hazardousLedgerService.queryLedger(queryDTO);
        
        // Then
        assertThat(result).hasSize(1);
        HazardousLedgerDTO ledger = result.get(0);
        assertThat(ledger.getClosingStock()).isEqualByComparingTo(new BigDecimal("70"));
        assertThat(ledger.getDiscrepancyRate()).isEqualByComparingTo(new BigDecimal("14.29"));
    }
    
    @Test
    @DisplayName("导出Excel - 正常情况")
    void testExportLedgerToExcel_Normal() {
        // Given
        List<MaterialInfo> materials = List.of(hazardousMaterial);
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        
        when(stockInDetailMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(stockOutDetailMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(approvalClient.getUnreturnedQuantity(any())).thenReturn(BigDecimal.ZERO);
        
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        
        // When
        byte[] excelBytes = hazardousLedgerService.exportLedgerToExcel(queryDTO);
        
        // Then
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes.length).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("管控类型文本转换")
    void testControlTypeText() {
        // Given
        List<MaterialInfo> materials = new ArrayList<>();
        
        MaterialInfo material1 = new MaterialInfo();
        material1.setId(1L);
        material1.setMaterialName("易制毒药品");
        material1.setMaterialType(3);
        material1.setIsControlled(1);
        material1.setUnit("kg");
        materials.add(material1);
        
        MaterialInfo material2 = new MaterialInfo();
        material2.setId(2L);
        material2.setMaterialName("易制爆药品");
        material2.setMaterialType(3);
        material2.setIsControlled(2);
        material2.setUnit("kg");
        materials.add(material2);
        
        when(materialClient.getHazardousMaterials()).thenReturn(materials);
        when(stockInDetailMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(stockOutDetailMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(approvalClient.getUnreturnedQuantity(any())).thenReturn(BigDecimal.ZERO);
        
        HazardousLedgerQueryDTO queryDTO = new HazardousLedgerQueryDTO();
        
        // When
        List<HazardousLedgerDTO> result = hazardousLedgerService.queryLedger(queryDTO);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getControlType()).isEqualTo(1);
        assertThat(result.get(1).getControlType()).isEqualTo(2);
    }
}
