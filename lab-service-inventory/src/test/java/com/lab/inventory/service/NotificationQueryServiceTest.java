package com.lab.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.dto.NotificationPageDTO;
import com.lab.inventory.dto.NotificationQueryDTO;
import com.lab.inventory.entity.Notification;
import com.lab.inventory.mapper.NotificationMapper;
import com.lab.inventory.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 通知查询服务单元测试
 * 
 * 测试通知查询、标记已读等功能
 */
class NotificationQueryServiceTest {
    
    @Mock
    private NotificationMapper notificationMapper;
    
    @InjectMocks
    private NotificationServiceImpl notificationService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testQueryNotifications() {
        // 准备测试数据
        NotificationQueryDTO queryDTO = new NotificationQueryDTO();
        queryDTO.setReceiverId(1L);
        queryDTO.setNotificationType(1);
        queryDTO.setIsRead(0);
        queryDTO.setPage(1);
        queryDTO.setSize(10);
        
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setReceiverId(1L);
        notification1.setNotificationType(1);
        notification1.setTitle("测试通知1");
        notification1.setContent("测试内容1");
        notification1.setIsRead(0);
        notification1.setPushChannel(1);
        notification1.setCreatedTime(LocalDateTime.now());
        
        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setReceiverId(1L);
        notification2.setNotificationType(1);
        notification2.setTitle("测试通知2");
        notification2.setContent("测试内容2");
        notification2.setIsRead(0);
        notification2.setPushChannel(1);
        notification2.setCreatedTime(LocalDateTime.now());
        
        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(notification1, notification2));
        page.setTotal(2);
        
        when(notificationMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);
        when(notificationMapper.selectCount(any(QueryWrapper.class)))
                .thenReturn(2L);
        
        // 执行测试
        NotificationPageDTO result = notificationService.queryNotifications(queryDTO);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2L, result.getUnreadCount());
        assertEquals(2, result.getList().size());
        assertEquals("测试通知1", result.getList().get(0).getTitle());
        assertEquals("审批通知", result.getList().get(0).getNotificationTypeDesc());
        assertEquals("站内消息", result.getList().get(0).getPushChannelDesc());
        
        // 验证mapper方法被调用
        verify(notificationMapper, times(1)).selectPage(any(Page.class), any(QueryWrapper.class));
        verify(notificationMapper, times(1)).selectCount(any(QueryWrapper.class));
    }
    
    @Test
    void testQueryNotificationsWithoutFilters() {
        // 测试不带过滤条件的查询
        NotificationQueryDTO queryDTO = new NotificationQueryDTO();
        queryDTO.setReceiverId(1L);
        queryDTO.setPage(1);
        queryDTO.setSize(10);
        
        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(new Notification()));
        page.setTotal(1);
        
        when(notificationMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(page);
        when(notificationMapper.selectCount(any(QueryWrapper.class)))
                .thenReturn(0L);
        
        NotificationPageDTO result = notificationService.queryNotifications(queryDTO);
        
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(0L, result.getUnreadCount());
    }
    
    @Test
    void testMarkAsRead() {
        // 准备测试数据
        Long notificationId = 1L;
        Long userId = 1L;
        
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setReceiverId(userId);
        notification.setIsRead(0);
        
        when(notificationMapper.selectById(notificationId)).thenReturn(notification);
        when(notificationMapper.updateById(any(Notification.class))).thenReturn(1);
        
        // 执行测试
        notificationService.markAsRead(notificationId, userId);
        
        // 验证mapper方法被调用
        verify(notificationMapper, times(1)).selectById(notificationId);
        verify(notificationMapper, times(1)).updateById(any(Notification.class));
    }
    
    @Test
    void testMarkAsReadNotificationNotFound() {
        // 测试通知不存在的情况
        Long notificationId = 1L;
        Long userId = 1L;
        
        when(notificationMapper.selectById(notificationId)).thenReturn(null);
        
        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificationService.markAsRead(notificationId, userId);
        });
        
        assertEquals("通知不存在", exception.getMessage());
        verify(notificationMapper, times(1)).selectById(notificationId);
        verify(notificationMapper, never()).updateById(any(Notification.class));
    }
    
    @Test
    void testMarkAsReadUnauthorized() {
        // 测试无权限操作的情况
        Long notificationId = 1L;
        Long userId = 1L;
        Long otherUserId = 2L;
        
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setReceiverId(otherUserId); // 不同的用户ID
        notification.setIsRead(0);
        
        when(notificationMapper.selectById(notificationId)).thenReturn(notification);
        
        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificationService.markAsRead(notificationId, userId);
        });
        
        assertEquals("无权限操作此通知", exception.getMessage());
        verify(notificationMapper, times(1)).selectById(notificationId);
        verify(notificationMapper, never()).updateById(any(Notification.class));
    }
    
    @Test
    void testMarkAsReadAlreadyRead() {
        // 测试已读通知的情况
        Long notificationId = 1L;
        Long userId = 1L;
        
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setReceiverId(userId);
        notification.setIsRead(1); // 已读
        
        when(notificationMapper.selectById(notificationId)).thenReturn(notification);
        
        // 执行测试
        notificationService.markAsRead(notificationId, userId);
        
        // 验证不会调用update方法
        verify(notificationMapper, times(1)).selectById(notificationId);
        verify(notificationMapper, never()).updateById(any(Notification.class));
    }
    
    @Test
    void testMarkAllAsRead() {
        // 准备测试数据
        Long userId = 1L;
        
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setReceiverId(userId);
        notification1.setIsRead(0);
        
        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setReceiverId(userId);
        notification2.setIsRead(0);
        
        Notification notification3 = new Notification();
        notification3.setId(3L);
        notification3.setReceiverId(userId);
        notification3.setIsRead(0);
        
        when(notificationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(notification1, notification2, notification3));
        when(notificationMapper.updateById(any(Notification.class))).thenReturn(1);
        
        // 执行测试
        notificationService.markAllAsRead(userId);
        
        // 验证mapper方法被调用
        verify(notificationMapper, times(1)).selectList(any(QueryWrapper.class));
        verify(notificationMapper, times(3)).updateById(any(Notification.class));
    }
    
    @Test
    void testGetUnreadCount() {
        // 准备测试数据
        Long userId = 1L;
        Long expectedCount = 5L;
        
        when(notificationMapper.selectCount(any(QueryWrapper.class)))
                .thenReturn(expectedCount);
        
        // 执行测试
        Long result = notificationService.getUnreadCount(userId);
        
        // 验证结果
        assertEquals(expectedCount, result);
        
        // 验证mapper方法被调用
        verify(notificationMapper, times(1)).selectCount(any(QueryWrapper.class));
    }
    
    @Test
    void testGetUnreadCountZero() {
        // 测试未读数量为0的情况
        Long userId = 1L;
        
        when(notificationMapper.selectCount(any(QueryWrapper.class)))
                .thenReturn(0L);
        
        Long result = notificationService.getUnreadCount(userId);
        
        assertEquals(0L, result);
    }
}
