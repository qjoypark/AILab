package com.lab.inventory.service;

import com.lab.inventory.dto.ConsumptionStatisticsDTO;
import com.lab.inventory.dto.StockSummaryDTO;
import com.lab.inventory.service.impl.ReportServiceImpl;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 报表导出服务测试
 */
@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {
    
    @Mock
    private ReportService mockReportService;
    
    @InjectMocks
    private ReportServiceImpl reportService;
    
    @BeforeEach
    void setUp() {
        // Mock the query methods to return test data
        when(mockReportService.getStockSummary(any(), any())).thenReturn(createMockStockSummary());
        when(mockReportService.getConsumptionStatistics(any(), any(), any())).thenReturn(createMockConsumptionStatistics());
    }
    
    @Test
    void testExportStockSummaryToExcel() throws Exception {
        // Given
        StockSummaryDTO mockSummary = createMockStockSummary();
        when(mockReportService.getStockSummary(null, null)).thenReturn(mockSummary);
        
        // When
        byte[] excelBytes = reportService.exportStockSummaryToExcel(null, null);
        
        // Then
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes.length).isGreaterThan(0);
        
        // Verify Excel content
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("库存汇总报表");
            
            // Verify header row
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("分类名称");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("物品数量");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("总库存数量");
            assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("总价值(元)");
            assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("价值占比(%)");
            
            // Verify data rows
            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("试剂");
            assertThat(dataRow.getCell(1).getNumericCellValue()).isEqualTo(10);
            
            // Verify summary row
            Row summaryRow = sheet.getRow(2);
            assertThat(summaryRow.getCell(0).getStringCellValue()).isEqualTo("总计");
            assertThat(summaryRow.getCell(3).getNumericCellValue()).isEqualTo(50000.0);
        }
    }
    
    @Test
    void testExportConsumptionStatisticsToExcel() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        ConsumptionStatisticsDTO mockStatistics = createMockConsumptionStatistics();
        when(mockReportService.getConsumptionStatistics(startDate, endDate, null)).thenReturn(mockStatistics);
        
        // When
        byte[] excelBytes = reportService.exportConsumptionStatisticsToExcel(startDate, endDate, null);
        
        // Then
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes.length).isGreaterThan(0);
        
        // Verify Excel content
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("消耗统计报表");
            
            // Verify header row
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("药品编码");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("药品名称");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("规格");
            assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("单位");
            assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("消耗数量");
            assertThat(headerRow.getCell(5).getStringCellValue()).isEqualTo("消耗成本(元)");
            assertThat(headerRow.getCell(6).getStringCellValue()).isEqualTo("成本占比(%)");
            
            // Verify data rows
            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("M001");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("无水乙醇");
            
            // Verify summary row
            Row summaryRow = sheet.getRow(2);
            assertThat(summaryRow.getCell(0).getStringCellValue()).isEqualTo("总计");
            assertThat(summaryRow.getCell(4).getNumericCellValue()).isEqualTo(100.0);
            assertThat(summaryRow.getCell(5).getNumericCellValue()).isEqualTo(5000.0);
        }
    }
    
    @Test
    void testExportStockSummaryWithEmptyData() throws Exception {
        // Given
        StockSummaryDTO emptySummary = new StockSummaryDTO();
        emptySummary.setTotalValue(BigDecimal.ZERO);
        emptySummary.setCategories(new ArrayList<>());
        when(mockReportService.getStockSummary(null, null)).thenReturn(emptySummary);
        
        // When
        byte[] excelBytes = reportService.exportStockSummaryToExcel(null, null);
        
        // Then
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes.length).isGreaterThan(0);
        
        // Verify Excel content
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("库存汇总报表");
            
            // Should have header row and summary row only
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
        }
    }
    
    @Test
    void testExportConsumptionStatisticsWithEmptyData() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        ConsumptionStatisticsDTO emptyStatistics = new ConsumptionStatisticsDTO();
        emptyStatistics.setTotalConsumption(BigDecimal.ZERO);
        emptyStatistics.setTotalCost(BigDecimal.ZERO);
        emptyStatistics.setMaterials(new ArrayList<>());
        when(mockReportService.getConsumptionStatistics(startDate, endDate, null)).thenReturn(emptyStatistics);
        
        // When
        byte[] excelBytes = reportService.exportConsumptionStatisticsToExcel(startDate, endDate, null);
        
        // Then
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes.length).isGreaterThan(0);
        
        // Verify Excel content
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("消耗统计报表");
            
            // Should have header row and summary row only
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
        }
    }
    
    private StockSummaryDTO createMockStockSummary() {
        StockSummaryDTO summary = new StockSummaryDTO();
        summary.setTotalValue(new BigDecimal("50000.00"));
        
        List<StockSummaryDTO.CategorySummary> categories = new ArrayList<>();
        
        StockSummaryDTO.CategorySummary category = new StockSummaryDTO.CategorySummary();
        category.setCategoryId(1L);
        category.setCategoryName("试剂");
        category.setItemCount(10);
        category.setTotalQuantity(new BigDecimal("500"));
        category.setTotalValue(new BigDecimal("50000.00"));
        category.setValuePercentage(new BigDecimal("100.00"));
        
        categories.add(category);
        summary.setCategories(categories);
        
        return summary;
    }
    
    private ConsumptionStatisticsDTO createMockConsumptionStatistics() {
        ConsumptionStatisticsDTO statistics = new ConsumptionStatisticsDTO();
        statistics.setTotalConsumption(new BigDecimal("100"));
        statistics.setTotalCost(new BigDecimal("5000.00"));
        
        List<ConsumptionStatisticsDTO.MaterialConsumption> materials = new ArrayList<>();
        
        ConsumptionStatisticsDTO.MaterialConsumption material = new ConsumptionStatisticsDTO.MaterialConsumption();
        material.setMaterialId(1L);
        material.setMaterialCode("M001");
        material.setMaterialName("无水乙醇");
        material.setSpecification("500ml");
        material.setUnit("瓶");
        material.setConsumptionQuantity(new BigDecimal("100"));
        material.setConsumptionCost(new BigDecimal("5000.00"));
        material.setCostRate(new BigDecimal("100.00"));
        
        materials.add(material);
        statistics.setMaterials(materials);
        
        return statistics;
    }
}
