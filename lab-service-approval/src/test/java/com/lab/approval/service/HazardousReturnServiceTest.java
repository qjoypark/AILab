package com.lab.approval.service;

import com.lab.approval.client.InventoryClient;
import com.lab.approval.dto.HazardousReturnRequest;
import com.lab.approval.entity.HazardousUsageRecord;
import com.lab.approval.mapper.HazardousUsageRecordMapper;
import com.lab.approval.service.impl.HazardousUsageRecordServiceImpl;
import com.lab.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 危化品归还服务测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("危化品归还服务测试")
class HazardousReturnServiceTest {
    
    @Mock
    private HazardousUsageRecordMapper usageRecordMapper;
    
    @Mock
    private InventoryClient inventoryClient;
    
    @InjectMocks
    private HazardousUsageRecordServiceImpl usageRecordService;
    
    private HazardousUsageRecord mockRecord;
    
    @BeforeEach
    void setUp() {
        mockRecord = new HazardousUsageRecord();
        mockRecord.setId(1L);
        mockRecord.setApplicationId(100L);
        mockRecord.setMaterialId(200L);
        mockRecord.setUserId(300L);
        mockRecord.setUserName("张三");
        mockRecord.setReceivedQuantity(new BigDecimal("10.00"));
        mockRecord.setStatus(1); // 使用中
        mockRecord.setUsageDate(LocalDate.now());
    }
    
    @Test
    @DisplayName("正常归还 - 全部归还")
    void testReturnHazardousMaterial_FullReturn() {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("3.00"));
        request.setReturnedQuantity(new BigDecimal("7.00"));
        request.setWasteQuantity(new BigDecimal("0.00"));
        request.setRemark("实验完成，全部归还");
        
        when(usageRecordMapper.selectById(1L)).thenReturn(mockRecord);
        when(inventoryClient.returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString()))
            .thenReturn(true);
        when(usageRecordMapper.updateById(any(HazardousUsageRecord.class))).thenReturn(1);
        
        // Act
        HazardousUsageRecord result = usageRecordService.returnHazardousMaterial(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("3.00"), result.getActualUsedQuantity());
        assertEquals(new BigDecimal("7.00"), result.getReturnedQuantity());
        assertEquals(new BigDecimal("0.00"), result.getWasteQuantity());
        assertEquals(2, result.getStatus()); // 已归还
        assertNotNull(result.getReturnDate());
        
        verify(inventoryClient).returnHazardousMaterial(200L, new BigDecimal("7.00"), anyString());
        verify(usageRecordMapper).updateById(any(HazardousUsageRecord.class));
    }
    
    @Test
    @DisplayName("正常归还 - 部分归还有废弃")
    void testReturnHazardousMaterial_PartialReturnWithWaste() {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("5.00"));
        request.setReturnedQuantity(new BigDecimal("3.00"));
        request.setWasteQuantity(new BigDecimal("2.00"));
        request.setRemark("部分废弃");
        
        when(usageRecordMapper.selectById(1L)).thenReturn(mockRecord);
        when(inventoryClient.returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString()))
            .thenReturn(true);
        when(usageRecordMapper.updateById(any(HazardousUsageRecord.class))).thenReturn(1);
        
        // Act
        HazardousUsageRecord result = usageRecordService.returnHazardousMaterial(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("5.00"), result.getActualUsedQuantity());
        assertEquals(new BigDecimal("3.00"), result.getReturnedQuantity());
        assertEquals(new BigDecimal("2.00"), result.getWasteQuantity());
        assertEquals(2, result.getStatus());
        
        verify(inventoryClient).returnHazardousMaterial(200L, new BigDecimal("3.00"), anyString());
    }
    
    @Test
    @DisplayName("正常归还 - 全部使用无归还")
    void testReturnHazardousMaterial_FullyUsedNoReturn() {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("10.00"));
        request.setReturnedQuantity(new BigDecimal("0.00"));
        request.setWasteQuantity(new BigDecimal("0.00"));
        request.setRemark("全部使用完毕");
        
        when(usageRecordMapper.selectById(1L)).thenReturn(mockRecord);
        when(usageRecordMapper.updateById(any(HazardousUsageRecord.class))).thenReturn(1);
        
        // Act
        HazardousUsageRecord result = usageRecordService.returnHazardousMaterial(1L, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("10.00"), result.getActualUsedQuantity());
        assertEquals(new BigDecimal("0.00"), result.getReturnedQuantity());
        assertEquals(2, result.getStatus());
        
        // 归还数量为0时，不应调用库存服务
        verify(inventoryClient, never()).returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString());
    }
    
    @Test
    @DisplayName("异常情况 - 使用记录不存在")
    void testReturnHazardousMaterial_RecordNotFound() {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("5.00"));
        request.setReturnedQuantity(new BigDecimal("5.00"));
        request.setWasteQuantity(new BigDecimal("0.00"));
        
        when(usageRecordMapper.selectById(999L)).thenReturn(null);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            usageRecordService.returnHazardousMaterial(999L, request);
        });
        
        assertEquals("使用记录不存在", exception.getMessage());
        verify(usageRecordMapper, never()).updateById(any());
    }
    
    @Test
    @DisplayName("异常情况 - 记录已归还")
    void testReturnHazardousMaterial_AlreadyReturned() {
        // Arrange
        mockRecord.setStatus(2); // 已归还
        
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("5.00"));
        request.setReturnedQuantity(new BigDecimal("5.00"));
        request.setWasteQuantity(new BigDecimal("0.00"));
        
        when(usageRecordMapper.selectById(1L)).thenReturn(mockRecord);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            usageRecordService.returnHazardousMaterial(1L, request);
        });
        
        assertEquals("该记录已归还，不能重复归还", exception.getMessage());
        verify(usageRecordMapper, never()).updateById(any());
    }
    
    @Test
    @DisplayName("异常情况 - 数量不匹配（总和大于领用数量）")
    void testReturnHazardousMaterial_QuantityMismatch_Greater() {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("5.00"));
        request.setReturnedQuantity(new BigDecimal("6.00"));
        request.setWasteQuantity(new BigDecimal("1.00"));
        // 总和 = 12.00，大于领用数量 10.00
        
        when(usageRecordMapper.selectById(1L)).thenReturn(mockRecord);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            usageRecordService.returnHazardousMaterial(1L, request);
        });
        
        assertTrue(exception.getMessage().contains("数量不匹配"));
        verify(usageRecordMapper, never()).updateById(any());
    }
    
    @Test
    @DisplayName("异常情况 - 数量不匹配（总和小于领用数量）")
    void testReturnHazardousMaterial_QuantityMismatch_Less() {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("3.00"));
        request.setReturnedQuantity(new BigDecimal("2.00"));
        request.setWasteQuantity(new BigDecimal("1.00"));
        // 总和 = 6.00，小于领用数量 10.00
        
        when(usageRecordMapper.selectById(1L)).thenReturn(mockRecord);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            usageRecordService.returnHazardousMaterial(1L, request);
        });
        
        assertTrue(exception.getMessage().contains("数量不匹配"));
        verify(usageRecordMapper, never()).updateById(any());
    }
    
    @Test
    @DisplayName("容错处理 - 库存服务调用失败但继续更新记录")
    void testReturnHazardousMaterial_InventoryServiceFailed() {
        // Arrange
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("5.00"));
        request.setReturnedQuantity(new BigDecimal("5.00"));
        request.setWasteQuantity(new BigDecimal("0.00"));
        
        when(usageRecordMapper.selectById(1L)).thenReturn(mockRecord);
        when(inventoryClient.returnHazardousMaterial(anyLong(), any(BigDecimal.class), anyString()))
            .thenReturn(false); // 库存服务调用失败
        when(usageRecordMapper.updateById(any(HazardousUsageRecord.class))).thenReturn(1);
        
        // Act
        HazardousUsageRecord result = usageRecordService.returnHazardousMaterial(1L, request);
        
        // Assert - 即使库存服务失败，使用记录仍应更新
        assertNotNull(result);
        assertEquals(2, result.getStatus());
        verify(usageRecordMapper).updateById(any(HazardousUsageRecord.class));
    }
    
    @Test
    @DisplayName("备注追加 - 原有备注存在")
    void testReturnHazardousMaterial_AppendRemark() {
        // Arrange
        mockRecord.setRemark("原有备注");
        
        HazardousReturnRequest request = new HazardousReturnRequest();
        request.setActualUsedQuantity(new BigDecimal("10.00"));
        request.setReturnedQuantity(new BigDecimal("0.00"));
        request.setWasteQuantity(new BigDecimal("0.00"));
        request.setRemark("新增备注");
        
        when(usageRecordMapper.selectById(1L)).thenReturn(mockRecord);
        when(usageRecordMapper.updateById(any(HazardousUsageRecord.class))).thenReturn(1);
        
        // Act
        HazardousUsageRecord result = usageRecordService.returnHazardousMaterial(1L, request);
        
        // Assert
        assertTrue(result.getRemark().contains("原有备注"));
        assertTrue(result.getRemark().contains("新增备注"));
        assertTrue(result.getRemark().contains(";"));
    }
}
