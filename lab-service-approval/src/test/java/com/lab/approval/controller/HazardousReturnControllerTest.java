package com.lab.approval.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.approval.dto.HazardousReturnRequest;
import com.lab.approval.entity.HazardousUsageRecord;
import com.lab.approval.service.HazardousUsageRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 危化品归还控制器测试
 */
@WebMvcTest(HazardousUsageRecordController.class)
@DisplayName("危化品归还控制器测试")
class HazardousReturnControllerTest extends BaseControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private HazardousUsageRecordService usageRecordService;
    
    @Test
    @DisplayName("POST /api/v1/hazardous/usage-records/{id}/return - 成功归还")
    void testReturnHazardousMaterial_Success() throws Exception {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("3.00"));
        request.setReturnedQuantity(new BigDecimal("7.00"));
        request.setWasteQuantity(new BigDecimal("0.00"));
        request.setRemark("实验完成");
        
        HazardousUsageRecord mockRecord = new HazardousUsageRecord();
        mockRecord.setId(1L);
        mockRecord.setMaterialId(200L);
        mockRecord.setUserId(300L);
        mockRecord.setUserName("张三");
        mockRecord.setReceivedQuantity(new BigDecimal("10.00"));
        mockRecord.setActualUsedQuantity(new BigDecimal("3.00"));
        mockRecord.setReturnedQuantity(new BigDecimal("7.00"));
        mockRecord.setWasteQuantity(new BigDecimal("0.00"));
        mockRecord.setStatus(2);
        mockRecord.setReturnDate(LocalDate.now());
        
        when(usageRecordService.returnHazardousMaterial(eq(1L), any(HazardousReturnRequest.class)))
            .thenReturn(mockRecord);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/hazardous/usage-records/1/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.actualUsedQuantity").value(3.00))
            .andExpect(jsonPath("$.data.returnedQuantity").value(7.00))
            .andExpect(jsonPath("$.data.wasteQuantity").value(0.00))
            .andExpect(jsonPath("$.data.status").value(2));
    }
    
    @Test
    @DisplayName("POST /api/v1/hazardous/usage-records/{id}/return - 验证失败（实际使用量为空）")
    void testReturnHazardousMaterial_ValidationFailed_NullActualUsed() throws Exception {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(null); // 缺少必填字段
        request.setReturnedQuantity(new BigDecimal("7.00"));
        request.setWasteQuantity(new BigDecimal("0.00"));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/hazardous/usage-records/1/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("POST /api/v1/hazardous/usage-records/{id}/return - 验证失败（归还数量为负数）")
    void testReturnHazardousMaterial_ValidationFailed_NegativeReturn() throws Exception {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("5.00"));
        request.setReturnedQuantity(new BigDecimal("-1.00")); // 负数
        request.setWasteQuantity(new BigDecimal("0.00"));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/hazardous/usage-records/1/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("POST /api/v1/hazardous/usage-records/{id}/return - 部分归还有废弃")
    void testReturnHazardousMaterial_PartialReturnWithWaste() throws Exception {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("5.00"));
        request.setReturnedQuantity(new BigDecimal("3.00"));
        request.setWasteQuantity(new BigDecimal("2.00"));
        request.setRemark("部分废弃");
        
        HazardousUsageRecord mockRecord = new HazardousUsageRecord();
        mockRecord.setId(1L);
        mockRecord.setReceivedQuantity(new BigDecimal("10.00"));
        mockRecord.setActualUsedQuantity(new BigDecimal("5.00"));
        mockRecord.setReturnedQuantity(new BigDecimal("3.00"));
        mockRecord.setWasteQuantity(new BigDecimal("2.00"));
        mockRecord.setStatus(2);
        
        when(usageRecordService.returnHazardousMaterial(eq(1L), any(HazardousReturnRequest.class)))
            .thenReturn(mockRecord);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/hazardous/usage-records/1/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.actualUsedQuantity").value(5.00))
            .andExpect(jsonPath("$.data.returnedQuantity").value(3.00))
            .andExpect(jsonPath("$.data.wasteQuantity").value(2.00));
    }
}
