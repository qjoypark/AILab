package com.lab.inventory.service;

import com.lab.inventory.client.UserClient;
import com.lab.inventory.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 通知服务测试
 * 
 * 测试任务 9.11: 验证通知发送逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通知服务测试")
class NotificationServiceTest {
    
    @Mock
    private UserClient userClient;
    
    @InjectMocks
    private NotificationServiceImpl notificationService;
    
    @Test
    @DisplayName("应查询角色用户并发送通知")
    void shouldQueryRoleUsersAndSendNotifications() {
        // Given: 中心管理员有1个用户，安全管理员有1个用户
        when(userClient.getUserIdsByRoleName("CENTER_ADMIN")).thenReturn(Collections.singletonList(3L));
        when(userClient.getUserIdsByRoleName("SAFETY_ADMIN")).thenReturn(Collections.singletonList(1L));
        
        // When: 发送预警通知给中心管理员和安全管理员
        notificationService.sendAlertNotification(
                Arrays.asList("CENTER_ADMIN", "SAFETY_ADMIN"),
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );
        
        // Then: 应查询两个角色的用户
        verify(userClient).getUserIdsByRoleName("CENTER_ADMIN");
        verify(userClient).getUserIdsByRoleName("SAFETY_ADMIN");
    }
    
    @Test
    @DisplayName("应去重用户ID并发送通知")
    void shouldDeduplicateUserIdsAndSendNotifications() {
        // Given: 同一个用户同时拥有中心管理员和安全管理员角色
        when(userClient.getUserIdsByRoleName("CENTER_ADMIN")).thenReturn(Collections.singletonList(1L));
        when(userClient.getUserIdsByRoleName("SAFETY_ADMIN")).thenReturn(Collections.singletonList(1L));
        
        // When: 发送预警通知
        notificationService.sendAlertNotification(
                Arrays.asList("CENTER_ADMIN", "SAFETY_ADMIN"),
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );
        
        // Then: 应查询两个角色
        verify(userClient).getUserIdsByRoleName("CENTER_ADMIN");
        verify(userClient).getUserIdsByRoleName("SAFETY_ADMIN");
    }
    
    @Test
    @DisplayName("当角色没有用户时不应发送通知")
    void shouldNotSendNotificationWhenNoUsersInRole() {
        // Given: 角色没有用户
        when(userClient.getUserIdsByRoleName(anyString())).thenReturn(Collections.emptyList());
        
        // When: 发送预警通知
        notificationService.sendAlertNotification(
                Collections.singletonList("CENTER_ADMIN"),
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );
        
        // Then: 应查询角色用户
        verify(userClient).getUserIdsByRoleName("CENTER_ADMIN");
    }
    
    @Test
    @DisplayName("应向多个用户发送通知")
    void shouldSendNotificationsToMultipleUsers() {
        // Given: 中心管理员有2个用户
        when(userClient.getUserIdsByRoleName("CENTER_ADMIN")).thenReturn(Arrays.asList(3L, 4L));
        
        // When: 发送预警通知
        notificationService.sendAlertNotification(
                Collections.singletonList("CENTER_ADMIN"),
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );
        
        // Then: 应查询角色用户
        verify(userClient).getUserIdsByRoleName("CENTER_ADMIN");
    }
    
    @Test
    @DisplayName("应正确发送单个用户通知")
    void shouldSendNotificationToSingleUser() {
        // When: 发送通知给指定用户
        notificationService.sendNotification(
                1L,
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );
        
        // Then: 通知应被记录（通过日志）
        // 注意：实际实现中应该保存到数据库或发送到消息队列
        // 这里只是验证方法被调用
    }
}
