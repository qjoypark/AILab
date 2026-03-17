package com.lab.inventory.controller;

import com.lab.inventory.dto.HazardousLedgerDTO;
import com.lab.inventory.dto.HazardousLedgerQueryDTO;
import com.lab.inventory.service.HazardousLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 危化品台账控制器测试
 */
@WebMvcTest(HazardousLedgerController.class)
@DisplayName("危化品台账控制器测试")
class HazardousLedgerControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private HazardousLedgerService hazardousLedgerService;
    
    private List<HazardousLedgerDTO> mockLedgerList;
    
    @BeforeEach
    void setUp() {
        mockLedgerList = new ArrayList<>();
        
        HazardousLedgerDTO ledger = new HazardousLedgerDTO();
        ledger.setMaterialId(1L);
        ledger.setMaterialName("盐酸");
        ledger.setCasNumber("7647-01-0");
        ledger.setDangerCategory("腐蚀品");
        ledger.setControlType(0);
        ledger.setUnit("L");
        ledger.setOpeningStock(new BigDecimal("50"));
        ledger.setTotalStockIn(new BigDecimal("100"));
        ledger.setTotalStockOut(new BigDecimal("30"));
        ledger.setClosingStock(new BigDecimal("120"));
        ledger.setDiscrepancyRate(new BigDecimal("5.50"));
        
        mockLedgerList.add(ledger);
    }
    
    @Test
    @DisplayName("查询危化品台账 - 无参数")
    void testQueryLedger_NoParams() throws Exception {
        // Given
        when(hazardousLedgerService.queryLedger(any(HazardousLedgerQueryDTO.class)))
                .thenReturn(mockLedgerList);
        
        // When & Then
        mockMvc.perform(get("/api/v1/hazardous/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].materialId").value(1))
                .andExpect(jsonPath("$.data[0].materialName").value("盐酸"))
                .andExpect(jsonPath("$.data[0].casNumber").value("7647-01-0"))
                .andExpect(jsonPath("$.data[0].dangerCategory").value("腐蚀品"))
                .andExpect(jsonPath("$.data[0].controlType").value(0))
                .andExpect(jsonPath("$.data[0].unit").value("L"))
                .andExpect(jsonPath("$.data[0].openingStock").value(50))
                .andExpect(jsonPath("$.data[0].totalStockIn").value(100))
                .andExpect(jsonPath("$.data[0].totalStockOut").value(30))
                .andExpect(jsonPath("$.data[0].closingStock").value(120))
                .andExpect(jsonPath("$.data[0].discrepancyRate").value(5.50));
    }
    
    @Test
    @DisplayName("查询危化品台账 - 带时间范围参数")
    void testQueryLedger_WithDateRange() throws Exception {
        // Given
        when(hazardousLedgerService.queryLedger(any(HazardousLedgerQueryDTO.class)))
                .thenReturn(mockLedgerList);
        
        // When & Then
        mockMvc.perform(get("/api/v1/hazardous/ledger")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].materialName").value("盐酸"));
    }
    
    @Test
    @DisplayName("查询危化品台账 - 带药品ID参数")
    void testQueryLedger_WithMaterialId() throws Exception {
        // Given
        when(hazardousLedgerService.queryLedger(any(HazardousLedgerQueryDTO.class)))
                .thenReturn(mockLedgerList);
        
        // When & Then
        mockMvc.perform(get("/api/v1/hazardous/ledger")
                        .param("materialId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].materialId").value(1));
    }
    
    @Test
    @DisplayName("查询危化品台账 - 所有参数")
    void testQueryLedger_WithAllParams() throws Exception {
        // Given
        when(hazardousLedgerService.queryLedger(any(HazardousLedgerQueryDTO.class)))
                .thenReturn(mockLedgerList);
        
        // When & Then
        mockMvc.perform(get("/api/v1/hazardous/ledger")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31")
                        .param("materialId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    @DisplayName("导出Excel - 正常情况")
    void testExportLedger_Normal() throws Exception {
        // Given
        byte[] mockExcelBytes = new byte[]{1, 2, 3, 4, 5};
        when(hazardousLedgerService.exportLedgerToExcel(any(HazardousLedgerQueryDTO.class)))
                .thenReturn(mockExcelBytes);
        
        // When & Then
        mockMvc.perform(get("/api/v1/hazardous/ledger/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(mockExcelBytes));
    }
    
    @Test
    @DisplayName("导出Excel - 带参数")
    void testExportLedger_WithParams() throws Exception {
        // Given
        byte[] mockExcelBytes = new byte[]{1, 2, 3, 4, 5};
        when(hazardousLedgerService.exportLedgerToExcel(any(HazardousLedgerQueryDTO.class)))
                .thenReturn(mockExcelBytes);
        
        // When & Then
        mockMvc.perform(get("/api/v1/hazardous/ledger/export")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31")
                        .param("materialId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().exists("Content-Disposition"));
    }
    
    @Test
    @DisplayName("查询危化品台账 - 空结果")
    void testQueryLedger_EmptyResult() throws Exception {
        // Given
        when(hazardousLedgerService.queryLedger(any(HazardousLedgerQueryDTO.class)))
                .thenReturn(new ArrayList<>());
        
        // When & Then
        mockMvc.perform(get("/api/v1/hazardous/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
