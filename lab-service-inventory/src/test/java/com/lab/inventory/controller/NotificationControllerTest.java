package com.lab.inventory.controller;

import com.lab.inventory.dto.NotificationDTO;
import com.lab.inventory.dto.NotificationPageDTO;
import com.lab.inventory.dto.NotificationQueryDTO;
import com.lab.inventory.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 通知控制器单元测试
 */
class NotificationControllerTest {
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private NotificationController notificationController;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testQueryNotifications() {
        // 准备测试数据
        Long receiverId = 1L;
        Integer notificationType = 1;
        Integer isRead = 0;
        Integer page = 1;
        Integer size = 10;
        
        NotificationDTO notification1 = new NotificationDTO();
        notification1.setId(1L);
        notification1.setReceiverId(receiverId);
        notification1.setNotificationType(notificationType);
        notification1.setTitle("测试通知1");
        notification1.setContent("测试内容1");
        notification1.setIsRead(0);
        notification1.setCreatedTime(LocalDateTime.now());
        
        NotificationDTO notification2 = new NotificationDTO();
        notification2.setId(2L);
        notification2.setReceiverId(receiverId);
        notification2.setNotificationType(notificationType);
        notification2.setTitle("测试通知2");
        notification2.setContent("测试内容2");
        notification2.setIsRead(0);
        notification2.setCreatedTime(LocalDateTime.now());
        
        List<NotificationDTO> notificationList = Arrays.asList(notification1, notification2);
        
        NotificationPageDTO pageDTO = NotificationPageDTO.builder()
                .total(2L)
                .unreadCount(2L)
                .list(notificationList)
                .build();
        
        when(notificationService.queryNotifications(any(NotificationQueryDTO.class)))
                .thenReturn(pageDTO);
        
        // 执行测试
        var result = notificationController.queryNotifications(
                receiverId, notificationType, isRead, page, size);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2L, result.getData().getTotal());
        assertEquals(2L, result.getData().getUnreadCount());
        assertEquals(2, result.getData().getList().size());
        
        // 验证服务方法被调用
        ArgumentCaptor<NotificationQueryDTO> captor = ArgumentCaptor.forClass(NotificationQueryDTO.class);
        verify(notificationService, times(1)).queryNotifications(captor.capture());
        
        NotificationQueryDTO capturedDTO = captor.getValue();
        assertEquals(receiverId, capturedDTO.getReceiverId());
        assertEquals(notificationType, capturedDTO.getNotificationType());
        assertEquals(isRead, capturedDTO.getIsRead());
        assertEquals(page, capturedDTO.getPage());
        assertEquals(size, capturedDTO.getSize());
    }
    
    @Test
    void testQueryNotificationsWithoutFilters() {
        // 测试不带过滤条件的查询
        Long receiverId = 1L;
        
        NotificationPageDTO pageDTO = NotificationPageDTO.builder()
                .total(5L)
                .unreadCount(3L)
                .list(Arrays.asList(new NotificationDTO()))
                .build();
        
        when(notificationService.queryNotifications(any(NotificationQueryDTO.class)))
                .thenReturn(pageDTO);
        
        var result = notificationController.queryNotifications(
                receiverId, null, null, 1, 10);
        
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(5L, result.getData().getTotal());
        assertEquals(3L, result.getData().getUnreadCount());
    }
    
    @Test
    void testMarkAsRead() {
        // 准备测试数据
        Long notificationId = 1L;
        Long userId = 1L;
        
        doNothing().when(notificationService).markAsRead(notificationId, userId);
        
        // 执行测试
        var result = notificationController.markAsRead(notificationId, userId);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(200, result.getCode());
        
        // 验证服务方法被调用
        verify(notificationService, times(1)).markAsRead(notificationId, userId);
    }
    
    @Test
    void testMarkAllAsRead() {
        // 准备测试数据
        Long userId = 1L;
        
        doNothing().when(notificationService).markAllAsRead(userId);
        
        // 执行测试
        var result = notificationController.markAllAsRead(userId);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(200, result.getCode());
        
        // 验证服务方法被调用
        verify(notificationService, times(1)).markAllAsRead(userId);
    }
    
    @Test
    void testGetUnreadCount() {
        // 准备测试数据
        Long userId = 1L;
        Long expectedCount = 5L;
        
        when(notificationService.getUnreadCount(userId)).thenReturn(expectedCount);
        
        // 执行测试
        var result = notificationController.getUnreadCount(userId);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(expectedCount, result.getData());
        
        // 验证服务方法被调用
        verify(notificationService, times(1)).getUnreadCount(userId);
    }
}
