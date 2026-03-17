package com.lab.inventory.service;

import com.lab.inventory.entity.AlertRecord;
import com.lab.inventory.mapper.AlertRecordMapper;
import com.lab.inventory.mapper.StockInventoryMapper;
import com.lab.inventory.service.impl.AlertServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 预警通知测试
 * 
 * 测试任务 9.11: 验证预警通知发送给中心管理员和安全管理员
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("预警通知测试")
class AlertNotificationTest {
    
    @Mock
    private AlertRecordMapper alertRecordMapper;
    
    @Mock
    private StockInventoryMapper stockInventoryMapper;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private AlertServiceImpl alertService;
    
    @Test
    @DisplayName("创建账实差异预警时应发送通知给中心管理员和安全管理员")
    void shouldSendNotificationToCenterAdminAndSafetyAdminForDiscrepancyAlert() {
        // Given: 准备创建账实差异预警
        when(alertRecordMapper.insert(any(AlertRecord.class))).thenReturn(1);
        
        // When: 创建账实差异预警
        Long alertId = alertService.createAlert(
                4, // alertType: 账实差异
                3, // alertLevel: 严重
                "HAZARDOUS_MATERIAL",
                1L,
                "危化品账实差异预警",
                "危化品: 浓硫酸\n账面库存: 100 瓶\n已领用未归还: 10 瓶\n实际库存: 90 瓶\n账实差异: 10.00%"
        );
        
        // Then: 应发送通知
        ArgumentCaptor<List<String>> roleNamesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> businessTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> businessIdCaptor = ArgumentCaptor.forClass(Long.class);
        
        verify(notificationService).sendAlertNotification(
                roleNamesCaptor.capture(),
                titleCaptor.capture(),
                contentCaptor.capture(),
                businessTypeCaptor.capture(),
                businessIdCaptor.capture()
        );
        
        // 验证通知发送给中心管理员和安全管理员
        List<String> roleNames = roleNamesCaptor.getValue();
        assertEquals(2, roleNames.size(), "应发送给2个角色");
        assertTrue(roleNames.contains("CENTER_ADMIN"), "应包含中心管理员");
        assertTrue(roleNames.contains("SAFETY_ADMIN"), "应包含安全管理员");
        
        // 验证通知内容
        assertEquals("危化品账实差异预警", titleCaptor.getValue());
        assertTrue(contentCaptor.getValue().contains("浓硫酸"));
        assertEquals("HAZARDOUS_MATERIAL", businessTypeCaptor.getValue());
        assertEquals(1L, businessIdCaptor.getValue());
        
        // 验证预警记录已创建
        assertNotNull(alertId);
        verify(alertRecordMapper).insert(any(AlertRecord.class));
    }
    
    @Test
    @DisplayName("创建低库存预警时应只发送通知给中心管理员")
    void shouldSendNotificationOnlyToCenterAdminForLowStockAlert() {
        // Given: 准备创建低库存预警
        when(alertRecordMapper.insert(any(AlertRecord.class))).thenReturn(1);
        
        // When: 创建低库存预警
        alertService.createAlert(
                1, // alertType: 低库存
                2, // alertLevel: 警告
                "STOCK_INVENTORY",
                100L,
                "低库存预警",
                "药品ID: 1, 当前可用库存: 50, 低于安全库存: 100"
        );
        
        // Then: 应只发送通知给中心管理员
        ArgumentCaptor<List<String>> roleNamesCaptor = ArgumentCaptor.forClass(List.class);
        
        verify(notificationService).sendAlertNotification(
                roleNamesCaptor.capture(),
                anyString(),
                anyString(),
                anyString(),
                anyLong()
        );
        
        List<String> roleNames = roleNamesCaptor.getValue();
        assertEquals(1, roleNames.size(), "应只发送给1个角色");
        assertTrue(roleNames.contains("CENTER_ADMIN"), "应包含中心管理员");
        assertFalse(roleNames.contains("SAFETY_ADMIN"), "不应包含安全管理员");
    }
    
    @Test
    @DisplayName("创建有效期预警时应只发送通知给中心管理员")
    void shouldSendNotificationOnlyToCenterAdminForExpirationAlert() {
        // Given: 准备创建有效期预警
        when(alertRecordMapper.insert(any(AlertRecord.class))).thenReturn(1);
        
        // When: 创建有效期预警
        alertService.createAlert(
                2, // alertType: 有效期
                2, // alertLevel: 警告
                "STOCK_INVENTORY",
                100L,
                "有效期预警",
                "药品ID: 1, 批次号: BATCH001, 有效期至: 2024-02-01, 剩余天数: 15天"
        );
        
        // Then: 应只发送通知给中心管理员
        ArgumentCaptor<List<String>> roleNamesCaptor = ArgumentCaptor.forClass(List.class);
        
        verify(notificationService).sendAlertNotification(
                roleNamesCaptor.capture(),
                anyString(),
                anyString(),
                anyString(),
                anyLong()
        );
        
        List<String> roleNames = roleNamesCaptor.getValue();
        assertEquals(1, roleNames.size(), "应只发送给1个角色");
        assertTrue(roleNames.contains("CENTER_ADMIN"), "应包含中心管理员");
    }
    
    @Test
    @DisplayName("创建异常消耗预警时应只发送通知给中心管理员")
    void shouldSendNotificationOnlyToCenterAdminForAbnormalConsumptionAlert() {
        // Given: 准备创建异常消耗预警
        when(alertRecordMapper.insert(any(AlertRecord.class))).thenReturn(1);
        
        // When: 创建异常消耗预警
        alertService.createAlert(
                3, // alertType: 异常消耗
                2, // alertLevel: 警告
                "MATERIAL",
                1L,
                "异常消耗预警",
                "药品: 浓硫酸, 今日消耗: 50瓶, 历史平均: 10瓶, 超出5倍"
        );
        
        // Then: 应只发送通知给中心管理员
        ArgumentCaptor<List<String>> roleNamesCaptor = ArgumentCaptor.forClass(List.class);
        
        verify(notificationService).sendAlertNotification(
                roleNamesCaptor.capture(),
                anyString(),
                anyString(),
                anyString(),
                anyLong()
        );
        
        List<String> roleNames = roleNamesCaptor.getValue();
        assertEquals(1, roleNames.size(), "应只发送给1个角色");
        assertTrue(roleNames.contains("CENTER_ADMIN"), "应包含中心管理员");
    }
    
    @Test
    @DisplayName("预警级别为严重时应正确设置")
    void shouldSetSevereLevelCorrectly() {
        // Given: 准备创建严重级别预警
        when(alertRecordMapper.insert(any(AlertRecord.class))).thenAnswer(invocation -> {
            AlertRecord alert = invocation.getArgument(0);
            assertEquals(3, alert.getAlertLevel(), "预警级别应为严重（3）");
            assertEquals(4, alert.getAlertType(), "预警类型应为账实差异（4）");
            assertEquals("HAZARDOUS_MATERIAL", alert.getBusinessType());
            assertEquals(1, alert.getStatus(), "状态应为未处理（1）");
            assertNotNull(alert.getAlertTime(), "预警时间不应为空");
            return 1;
        });
        
        // When: 创建严重级别的账实差异预警
        alertService.createAlert(
                4, // alertType: 账实差异
                3, // alertLevel: 严重
                "HAZARDOUS_MATERIAL",
                1L,
                "危化品账实差异预警",
                "差异超过5%"
        );
        
        // Then: 验证预警记录正确创建
        verify(alertRecordMapper).insert(any(AlertRecord.class));
    }
}
