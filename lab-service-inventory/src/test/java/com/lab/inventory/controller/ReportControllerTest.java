package com.lab.inventory.controller;

import com.lab.inventory.dto.StockSummaryDTO;
import com.lab.inventory.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 报表控制器测试
 */
@WebMvcTest(ReportController.class)
@DisplayName("报表控制器测试")
class ReportControllerTest extends BaseControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ReportService reportService;
    
    @Test
    @DisplayName("应该成功获取库存汇总报表")
    void shouldGetStockSummarySuccessfully() throws Exception {
        // Given
        StockSummaryDTO summary = new StockSummaryDTO();
        summary.setTotalValue(new BigDecimal("10000"));
        
        List<StockSummaryDTO.CategorySummary> categories = new ArrayList<>();
        StockSummaryDTO.CategorySummary category = new StockSummaryDTO.CategorySummary();
        category.setCategoryId(1L);
        category.setCategoryName("化学试剂");
        category.setItemCount(5);
        category.setTotalQuantity(new BigDecimal("100"));
        category.setTotalValue(new BigDecimal("10000"));
        category.setValuePercentage(new BigDecimal("100.00"));
        categories.add(category);
        
        summary.setCategories(categories);
        
        when(reportService.getStockSummary(any(), any())).thenReturn(summary);
        
        // When & Then
        mockMvc.perform(get("/api/v1/reports/stock-summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalValue").value(10000))
            .andExpect(jsonPath("$.data.categories").isArray())
            .andExpect(jsonPath("$.data.categories[0].categoryName").value("化学试剂"))
            .andExpect(jsonPath("$.data.categories[0].itemCount").value(5))
            .andExpect(jsonPath("$.data.categories[0].totalValue").value(10000));
    }
    
    @Test
    @DisplayName("应该支持按仓库ID过滤")
    void shouldFilterByWarehouseId() throws Exception {
        // Given
        StockSummaryDTO summary = new StockSummaryDTO();
        summary.setTotalValue(new BigDecimal("5000"));
        summary.setCategories(new ArrayList<>());
        
        when(reportService.getStockSummary(eq(1L), any())).thenReturn(summary);
        
        // When & Then
        mockMvc.perform(get("/api/v1/reports/stock-summary")
                .param("warehouseId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalValue").value(5000));
    }
    
    @Test
    @DisplayName("应该支持按物料类型过滤")
    void shouldFilterByMaterialType() throws Exception {
        // Given
        StockSummaryDTO summary = new StockSummaryDTO();
        summary.setTotalValue(new BigDecimal("3000"));
        summary.setCategories(new ArrayList<>());
        
        when(reportService.getStockSummary(any(), eq(2))).thenReturn(summary);
        
        // When & Then
        mockMvc.perform(get("/api/v1/reports/stock-summary")
                .param("materialType", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalValue").value(3000));
    }
}
