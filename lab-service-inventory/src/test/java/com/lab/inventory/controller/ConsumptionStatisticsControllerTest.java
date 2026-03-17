package com.lab.inventory.controller;

import com.lab.inventory.dto.ConsumptionStatisticsDTO;
import com.lab.inventory.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 消耗统计控制器测试
 * 
 * **验证需求: 5.7, 10.2**
 */
@WebMvcTest(ReportController.class)
class ConsumptionStatisticsControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ReportService reportService;
    
    @Test
    void testGetConsumptionStatistics_Success() throws Exception {
        // Given: 准备返回数据
        ConsumptionStatisticsDTO dto = new ConsumptionStatisticsDTO();
        dto.setTotalConsumption(new BigDecimal("100.00"));
        dto.setTotalCost(new BigDecimal("500.00"));
        
        ConsumptionStatisticsDTO.MaterialConsumption material1 = new ConsumptionStatisticsDTO.MaterialConsumption();
        material1.setMaterialId(100L);
        material1.setMaterialName("试剂A");
        material1.setMaterialCode("REG-001");
        material1.setSpecification("500ml");
        material1.setUnit("瓶");
        material1.setConsumptionQuantity(new BigDecimal("50.00"));
        material1.setConsumptionCost(new BigDecimal("300.00"));
        material1.setCostRate(new BigDecimal("60.00"));
        
        ConsumptionStatisticsDTO.MaterialConsumption material2 = new ConsumptionStatisticsDTO.MaterialConsumption();
        material2.setMaterialId(101L);
        material2.setMaterialName("试剂B");
        material2.setMaterialCode("REG-002");
        material2.setSpecification("1L");
        material2.setUnit("瓶");
        material2.setConsumptionQuantity(new BigDecimal("50.00"));
        material2.setConsumptionCost(new BigDecimal("200.00"));
        material2.setCostRate(new BigDecimal("40.00"));
        
        dto.setMaterials(Arrays.asList(material1, material2));
        
        when(reportService.getConsumptionStatistics(any(LocalDate.class), any(LocalDate.class), eq(null)))
            .thenReturn(dto);
        
        // When & Then: 调用接口并验证响应
        mockMvc.perform(get("/api/v1/reports/consumption-statistics")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalConsumption").value(100.00))
            .andExpect(jsonPath("$.data.totalCost").value(500.00))
            .andExpect(jsonPath("$.data.materials.length()").value(2))
            .andExpect(jsonPath("$.data.materials[0].materialId").value(100))
            .andExpect(jsonPath("$.data.materials[0].materialName").value("试剂A"))
            .andExpect(jsonPath("$.data.materials[0].consumptionQuantity").value(50.00))
            .andExpect(jsonPath("$.data.materials[0].consumptionCost").value(300.00))
            .andExpect(jsonPath("$.data.materials[0].costRate").value(60.00))
            .andExpect(jsonPath("$.data.materials[1].materialId").value(101))
            .andExpect(jsonPath("$.data.materials[1].materialName").value("试剂B"));
    }
    
    @Test
    void testGetConsumptionStatistics_WithMaterialTypeFilter() throws Exception {
        // Given: 准备过滤后的数据
        ConsumptionStatisticsDTO dto = new ConsumptionStatisticsDTO();
        dto.setTotalConsumption(new BigDecimal("50.00"));
        dto.setTotalCost(new BigDecimal("300.00"));
        
        ConsumptionStatisticsDTO.MaterialConsumption material = new ConsumptionStatisticsDTO.MaterialConsumption();
        material.setMaterialId(100L);
        material.setMaterialName("试剂A");
        material.setConsumptionQuantity(new BigDecimal("50.00"));
        material.setConsumptionCost(new BigDecimal("300.00"));
        material.setCostRate(new BigDecimal("100.00"));
        
        dto.setMaterials(Arrays.asList(material));
        
        when(reportService.getConsumptionStatistics(any(LocalDate.class), any(LocalDate.class), eq(2)))
            .thenReturn(dto);
        
        // When & Then: 调用接口并验证响应
        mockMvc.perform(get("/api/v1/reports/consumption-statistics")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("materialType", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalConsumption").value(50.00))
            .andExpect(jsonPath("$.data.totalCost").value(300.00))
            .andExpect(jsonPath("$.data.materials.length()").value(1))
            .andExpect(jsonPath("$.data.materials[0].materialId").value(100));
    }
    
    @Test
    void testGetConsumptionStatistics_EmptyResult() throws Exception {
        // Given: 准备空结果
        ConsumptionStatisticsDTO dto = new ConsumptionStatisticsDTO();
        dto.setTotalConsumption(BigDecimal.ZERO);
        dto.setTotalCost(BigDecimal.ZERO);
        dto.setMaterials(Arrays.asList());
        
        when(reportService.getConsumptionStatistics(any(LocalDate.class), any(LocalDate.class), eq(null)))
            .thenReturn(dto);
        
        // When & Then: 调用接口并验证响应
        mockMvc.perform(get("/api/v1/reports/consumption-statistics")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalConsumption").value(0))
            .andExpect(jsonPath("$.data.totalCost").value(0))
            .andExpect(jsonPath("$.data.materials.length()").value(0));
    }
    
    @Test
    void testGetConsumptionStatistics_MissingRequiredParameters() throws Exception {
        // When & Then: 缺少必需参数应返回400错误
        mockMvc.perform(get("/api/v1/reports/consumption-statistics")
                .param("startDate", "2024-01-01"))
            .andExpect(status().isBadRequest());
        
        mockMvc.perform(get("/api/v1/reports/consumption-statistics")
                .param("endDate", "2024-01-31"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetConsumptionStatistics_InvalidDateFormat() throws Exception {
        // When & Then: 无效的日期格式应返回400错误
        mockMvc.perform(get("/api/v1/reports/consumption-statistics")
                .param("startDate", "invalid-date")
                .param("endDate", "2024-01-31"))
            .andExpect(status().isBadRequest());
    }
}
