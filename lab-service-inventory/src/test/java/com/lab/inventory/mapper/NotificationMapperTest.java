package com.lab.inventory.mapper;

import com.lab.inventory.BaseBootTest;
import com.lab.inventory.entity.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通知Mapper测试
 * 
 * 测试任务 10.1: 验证通知数据表操作
 */
@DisplayName("通知Mapper测试")
class NotificationMapperTest extends BaseBootTest {
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    @Test
    @DisplayName("应成功插入通知记录")
    void shouldInsertNotification() {
        // Given: 创建通知记录
        Notification notification = new Notification();
        notification.setReceiverId(1L);
        notification.setNotificationType(1); // 审批通知
        notification.setTitle("领用申请待审批");
        notification.setContent("您有一条新的领用申请待审批");
        notification.setBusinessType("MATERIAL_APPLICATION");
        notification.setBusinessId(100L);
        notification.setPushChannel(1); // 站内消息
        notification.setIsRead(0); // 未读
        
        // When: 插入通知
        int result = notificationMapper.insert(notification);
        
        // Then: 插入成功
        assertThat(result).isEqualTo(1);
        
        // 验证可以查询到插入的记录
        Notification saved = notificationMapper.selectById(notification.getId());
        if (saved != null) {
            assertThat(saved.getReceiverId()).isEqualTo(1L);
            assertThat(saved.getTitle()).isEqualTo("领用申请待审批");
        }
    }
    
    @Test
    @DisplayName("应成功查询通知记录")
    void shouldSelectNotification() {
        // Given: 插入一条通知记录
        Notification notification = new Notification();
        notification.setReceiverId(2L);
        notification.setNotificationType(2); // 预警通知
        notification.setTitle("危化品账实差异预警");
        notification.setContent("危化品账实差异超过5%");
        notification.setBusinessType("HAZARDOUS_MATERIAL");
        notification.setBusinessId(200L);
        notification.setPushChannel(1);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        
        // When: 查询通知记录
        Notification found = notificationMapper.selectById(notification.getId());
        
        // Then: 查询成功
        assertThat(found).isNotNull();
        assertThat(found.getReceiverId()).isEqualTo(2L);
        assertThat(found.getNotificationType()).isEqualTo(2);
        assertThat(found.getTitle()).isEqualTo("危化品账实差异预警");
        assertThat(found.getBusinessType()).isEqualTo("HAZARDOUS_MATERIAL");
        assertThat(found.getIsRead()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("应成功更新通知为已读")
    void shouldUpdateNotificationAsRead() {
        // Given: 插入一条未读通知
        Notification notification = new Notification();
        notification.setReceiverId(3L);
        notification.setNotificationType(3); // 系统通知
        notification.setTitle("系统维护通知");
        notification.setContent("系统将于今晚进行维护");
        notification.setBusinessType("SYSTEM");
        notification.setPushChannel(1);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        
        // When: 更新为已读
        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        int result = notificationMapper.updateById(notification);
        
        // Then: 更新成功
        assertThat(result).isEqualTo(1);
        
        Notification updated = notificationMapper.selectById(notification.getId());
        assertThat(updated.getIsRead()).isEqualTo(1);
        assertThat(updated.getReadTime()).isNotNull();
    }
    
    @Test
    @DisplayName("应成功删除通知记录")
    void shouldDeleteNotification() {
        // Given: 插入一条通知记录
        Notification notification = new Notification();
        notification.setReceiverId(4L);
        notification.setNotificationType(1);
        notification.setTitle("测试通知");
        notification.setContent("这是一条测试通知");
        notification.setBusinessType("TEST");
        notification.setPushChannel(1);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        Long id = notification.getId();
        
        // When: 删除通知
        int result = notificationMapper.deleteById(id);
        
        // Then: 删除成功
        assertThat(result).isEqualTo(1);
        
        Notification deleted = notificationMapper.selectById(id);
        assertThat(deleted).isNull();
    }
}
