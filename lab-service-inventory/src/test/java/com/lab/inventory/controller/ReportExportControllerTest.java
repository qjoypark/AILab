package com.lab.inventory.controller;

import com.lab.inventory.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 报表导出控制器测试
 */
@WebMvcTest(ReportController.class)
class ReportExportControllerTest extends BaseControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ReportService reportService;
    
    @Test
    void testExportStockSummary() throws Exception {
        // Given
        byte[] mockExcelBytes = "mock excel content".getBytes();
        when(reportService.exportStockSummaryToExcel(null, null)).thenReturn(mockExcelBytes);
        
        // When & Then
        mockMvc.perform(get("/api/v1/reports/stock-summary/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(mockExcelBytes));
    }
    
    @Test
    void testExportStockSummaryWithParameters() throws Exception {
        // Given
        Long warehouseId = 1L;
        Integer materialType = 2;
        byte[] mockExcelBytes = "mock excel content".getBytes();
        when(reportService.exportStockSummaryToExcel(warehouseId, materialType)).thenReturn(mockExcelBytes);
        
        // When & Then
        mockMvc.perform(get("/api/v1/reports/stock-summary/export")
                        .param("warehouseId", warehouseId.toString())
                        .param("materialType", materialType.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(mockExcelBytes));
    }
    
    @Test
    void testExportConsumptionStatistics() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        byte[] mockExcelBytes = "mock excel content".getBytes();
        when(reportService.exportConsumptionStatisticsToExcel(startDate, endDate, null)).thenReturn(mockExcelBytes);
        
        // When & Then
        mockMvc.perform(get("/api/v1/reports/consumption-statistics/export")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(mockExcelBytes));
    }
    
    @Test
    void testExportConsumptionStatisticsWithMaterialType() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        Integer materialType = 3;
        byte[] mockExcelBytes = "mock excel content".getBytes();
        when(reportService.exportConsumptionStatisticsToExcel(startDate, endDate, materialType)).thenReturn(mockExcelBytes);
        
        // When & Then
        mockMvc.perform(get("/api/v1/reports/consumption-statistics/export")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("materialType", materialType.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(mockExcelBytes));
    }
    
    @Test
    void testExportConsumptionStatisticsMissingRequiredParameters() throws Exception {
        // When & Then - Missing startDate
        mockMvc.perform(get("/api/v1/reports/consumption-statistics/export")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isBadRequest());
        
        // When & Then - Missing endDate
        mockMvc.perform(get("/api/v1/reports/consumption-statistics/export")
                        .param("startDate", "2024-01-01"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testExportConsumptionStatisticsInvalidDateFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/reports/consumption-statistics/export")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isBadRequest());
    }
}
