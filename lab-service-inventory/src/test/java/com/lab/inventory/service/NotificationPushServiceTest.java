package com.lab.inventory.service;

import com.lab.inventory.client.UserClient;
import com.lab.inventory.dto.NotificationMessage;
import com.lab.inventory.enums.NotificationType;
import com.lab.inventory.enums.PushChannel;
import com.lab.inventory.mq.NotificationProducer;
import com.lab.inventory.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 通知推送服务单元测试
 * 
 * 测试消息推送服务的核心功能：
 * 1. 消息推送逻辑
 * 2. RabbitMQ异步发送
 * 3. 多种消息类型支持（审批、预警、系统）
 * 4. 批量发送给角色用户
 * 
 * **验证需求: 6.4, 18.4**
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通知推送服务测试")
class NotificationPushServiceTest {
    
    @Mock
    private UserClient userClient;
    
    @Mock
    private NotificationProducer notificationProducer;
    
    @InjectMocks
    private NotificationServiceImpl notificationService;
    
    @BeforeEach
    void setUp() {
        // 初始化测试数据
    }
    
    // ==================== 消息推送逻辑测试 ====================
    
    @Test
    @DisplayName("发送单个用户通知 - 应该发送到消息队列")
    void testSendNotificationToSingleUser() {
        // Given
        Long userId = 1L;
        String title = "测试通知";
        String content = "这是一条测试通知";
        String businessType = "TEST";
        Long businessId = 100L;
        
        // When
        notificationService.sendNotification(userId, title, content, businessType, businessId);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer, times(1)).sendNotification(messageCaptor.capture());
        
        NotificationMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getReceiverId()).isEqualTo(userId);
        assertThat(capturedMessage.getTitle()).isEqualTo(title);
        assertThat(capturedMessage.getContent()).isEqualTo(content);
        assertThat(capturedMessage.getBusinessType()).isEqualTo(businessType);
        assertThat(capturedMessage.getBusinessId()).isEqualTo(businessId);
        assertThat(capturedMessage.getNotificationType()).isEqualTo(NotificationType.SYSTEM.getCode());
        assertThat(capturedMessage.getPushChannel()).isEqualTo(PushChannel.IN_APP.getCode());
    }
    
    @Test
    @DisplayName("发送通知时包含完整业务信息 - 应该正确传递所有字段")
    void testSendNotificationWithCompleteBusinessInfo() {
        // Given
        Long userId = 10L;
        String title = "申请审批通知";
        String content = "您有新的危化品申请待审批";
        String businessType = "HAZARDOUS_APPLICATION";
        Long businessId = 999L;
        
        // When
        notificationService.sendNotification(userId, title, content, businessType, businessId);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer).sendNotification(messageCaptor.capture());
        
        NotificationMessage message = messageCaptor.getValue();
        assertThat(message.getReceiverId()).isEqualTo(userId);
        assertThat(message.getTitle()).isEqualTo(title);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getBusinessType()).isEqualTo(businessType);
        assertThat(message.getBusinessId()).isEqualTo(businessId);
    }
    
    // ==================== 不同通知类型测试 ====================
    
    @Test
    @DisplayName("发送审批通知 - 应该使用审批类型")
    void testSendApprovalNotification() {
        // Given
        Long userId = 1L;
        String title = "待审批通知";
        String content = "您有新的申请待审批";
        String businessType = "APPROVAL";
        Long businessId = 300L;
        
        // When
        notificationService.sendApprovalNotification(userId, title, content, businessType, businessId);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer, times(1)).sendNotification(messageCaptor.capture());
        
        NotificationMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getNotificationType()).isEqualTo(NotificationType.APPROVAL.getCode());
        assertThat(capturedMessage.getReceiverId()).isEqualTo(userId);
        assertThat(capturedMessage.getTitle()).isEqualTo(title);
    }
    
    @Test
    @DisplayName("发送预警通知 - 应该使用预警类型")
    void testSendAlertNotificationType() {
        // Given
        Long userId = 2L;
        String title = "库存预警";
        String content = "试剂库存不足";
        
        // When
        notificationService.sendNotificationWithType(userId, NotificationType.ALERT, 
                title, content, "STOCK_ALERT", 1L, PushChannel.IN_APP);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer).sendNotification(messageCaptor.capture());
        
        NotificationMessage message = messageCaptor.getValue();
        assertThat(message.getNotificationType()).isEqualTo(NotificationType.ALERT.getCode());
    }
    
    @Test
    @DisplayName("发送系统通知 - 应该使用系统类型")
    void testSendSystemNotificationType() {
        // Given
        Long userId = 3L;
        String title = "系统维护通知";
        String content = "系统将于今晚进行维护";
        
        // When
        notificationService.sendNotificationWithType(userId, NotificationType.SYSTEM, 
                title, content, "SYSTEM_MAINTENANCE", 1L, PushChannel.IN_APP);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer).sendNotification(messageCaptor.capture());
        
        NotificationMessage message = messageCaptor.getValue();
        assertThat(message.getNotificationType()).isEqualTo(NotificationType.SYSTEM.getCode());
    }
    
    // ==================== 批量发送给角色用户测试 ====================
    
    @Test
    @DisplayName("发送预警通知给角色用户 - 应该发送给所有角色用户")
    void testSendAlertNotificationToRoleUsers() {
        // Given
        List<String> roleNames = Arrays.asList("CENTER_ADMIN", "LAB_MANAGER");
        String title = "低库存预警";
        String content = "试剂A库存不足";
        String businessType = "STOCK_ALERT";
        Long businessId = 200L;
        
        // Mock用户查询
        when(userClient.getUserIdsByRoleName("CENTER_ADMIN"))
                .thenReturn(Arrays.asList(1L, 2L));
        when(userClient.getUserIdsByRoleName("LAB_MANAGER"))
                .thenReturn(Arrays.asList(2L, 3L)); // 用户2重复
        
        // When
        notificationService.sendAlertNotification(roleNames, title, content, businessType, businessId);
        
        // Then
        // 应该发送3条消息（用户1, 2, 3，去重后）
        verify(notificationProducer, times(3)).sendNotification(any(NotificationMessage.class));
        
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer, atLeastOnce()).sendNotification(messageCaptor.capture());
        
        List<NotificationMessage> messages = messageCaptor.getAllValues();
        assertThat(messages).hasSize(3);
        assertThat(messages).allMatch(msg -> msg.getNotificationType().equals(NotificationType.ALERT.getCode()));
        assertThat(messages).allMatch(msg -> msg.getTitle().equals(title));
    }
    
    @Test
    @DisplayName("发送预警通知但角色无用户 - 应该不发送消息")
    void testSendAlertNotificationWithNoUsers() {
        // Given
        List<String> roleNames = Collections.singletonList("UNKNOWN_ROLE");
        String title = "测试预警";
        String content = "测试内容";
        
        // Mock用户查询返回空列表
        when(userClient.getUserIdsByRoleName("UNKNOWN_ROLE"))
                .thenReturn(Collections.emptyList());
        
        // When
        notificationService.sendAlertNotification(roleNames, title, content, "TEST", 1L);
        
        // Then
        verify(notificationProducer, never()).sendNotification(any());
    }
    
    @Test
    @DisplayName("批量发送通知 - 应该为每个用户发送一条消息")
    void testBatchSendNotifications() {
        // Given
        List<String> roleNames = Collections.singletonList("CENTER_ADMIN");
        when(userClient.getUserIdsByRoleName("CENTER_ADMIN"))
                .thenReturn(Arrays.asList(1L, 2L, 3L, 4L, 5L));
        
        // When
        notificationService.sendAlertNotification(roleNames, "批量通知", "内容", "TEST", 1L);
        
        // Then
        verify(notificationProducer, times(5)).sendNotification(any(NotificationMessage.class));
    }
    
    @Test
    @DisplayName("发送给多个角色时去重用户 - 应该每个用户只收到一条消息")
    void testSendToMultipleRolesWithDuplicateUsers() {
        // Given
        List<String> roleNames = Arrays.asList("CENTER_ADMIN", "LAB_MANAGER", "SAFETY_ADMIN");
        
        // Mock: 三个角色有重叠用户
        when(userClient.getUserIdsByRoleName("CENTER_ADMIN"))
                .thenReturn(Arrays.asList(1L, 2L, 3L));
        when(userClient.getUserIdsByRoleName("LAB_MANAGER"))
                .thenReturn(Arrays.asList(2L, 3L, 4L));
        when(userClient.getUserIdsByRoleName("SAFETY_ADMIN"))
                .thenReturn(Arrays.asList(3L, 4L, 5L));
        
        // When
        notificationService.sendAlertNotification(roleNames, "危化品预警", "账实差异超标", "HAZARDOUS_ALERT", 1L);
        
        // Then
        // 用户1,2,3,4,5 共5个不重复用户
        verify(notificationProducer, times(5)).sendNotification(any(NotificationMessage.class));
    }
    
    // ==================== 推送渠道测试 ====================
    
    @Test
    @DisplayName("发送不同推送渠道的通知 - 应该正确设置推送渠道")
    void testSendNotificationWithDifferentChannels() {
        // Given
        Long userId = 1L;
        String title = "测试通知";
        String content = "测试内容";
        
        // When - 发送站内消息
        notificationService.sendNotificationWithType(userId, NotificationType.SYSTEM, 
                title, content, "TEST", 1L, PushChannel.IN_APP);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer, times(1)).sendNotification(messageCaptor.capture());
        
        NotificationMessage message = messageCaptor.getValue();
        assertThat(message.getPushChannel()).isEqualTo(PushChannel.IN_APP.getCode());
    }
    
    @Test
    @DisplayName("发送微信推送通知 - 应该设置微信渠道")
    void testSendWeChatNotification() {
        // Given
        Long userId = 1L;
        
        // When
        notificationService.sendNotificationWithType(userId, NotificationType.ALERT, 
                "紧急预警", "危化品异常", "ALERT", 1L, PushChannel.WECHAT);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer).sendNotification(messageCaptor.capture());
        
        assertThat(messageCaptor.getValue().getPushChannel()).isEqualTo(PushChannel.WECHAT.getCode());
    }
    
    // ==================== RabbitMQ消息发送测试 ====================
    
    @Test
    @DisplayName("消息发送到RabbitMQ - 应该调用NotificationProducer")
    void testMessageSentToRabbitMQ() {
        // Given
        Long userId = 1L;
        String title = "测试";
        String content = "内容";
        
        // When
        notificationService.sendNotification(userId, title, content, "TEST", 1L);
        
        // Then
        verify(notificationProducer, times(1)).sendNotification(any(NotificationMessage.class));
    }
    
    @Test
    @DisplayName("多次发送消息 - 应该每次都调用NotificationProducer")
    void testMultipleMessagesSentToRabbitMQ() {
        // Given
        Long userId = 1L;
        
        // When
        notificationService.sendNotification(userId, "通知1", "内容1", "TEST", 1L);
        notificationService.sendNotification(userId, "通知2", "内容2", "TEST", 2L);
        notificationService.sendNotification(userId, "通知3", "内容3", "TEST", 3L);
        
        // Then
        verify(notificationProducer, times(3)).sendNotification(any(NotificationMessage.class));
    }
    
    // ==================== 边界情况测试 ====================
    
    @Test
    @DisplayName("发送通知时业务ID为null - 应该正常发送")
    void testSendNotificationWithNullBusinessId() {
        // Given
        Long userId = 1L;
        
        // When
        notificationService.sendNotification(userId, "通知", "内容", "TEST", null);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer).sendNotification(messageCaptor.capture());
        
        assertThat(messageCaptor.getValue().getBusinessId()).isNull();
    }
    
    @Test
    @DisplayName("发送空角色列表 - 应该不发送消息")
    void testSendAlertNotificationWithEmptyRoleList() {
        // Given
        List<String> roleNames = Collections.emptyList();
        
        // When
        notificationService.sendAlertNotification(roleNames, "标题", "内容", "TEST", 1L);
        
        // Then
        verify(notificationProducer, never()).sendNotification(any());
        verify(userClient, never()).getUserIdsByRoleName(any());
    }
    
    @Test
    @DisplayName("发送给单个角色的单个用户 - 应该发送一条消息")
    void testSendToSingleRoleSingleUser() {
        // Given
        List<String> roleNames = Collections.singletonList("LAB_MANAGER");
        when(userClient.getUserIdsByRoleName("LAB_MANAGER"))
                .thenReturn(Collections.singletonList(100L));
        
        // When
        notificationService.sendAlertNotification(roleNames, "通知", "内容", "TEST", 1L);
        
        // Then
        verify(notificationProducer, times(1)).sendNotification(any(NotificationMessage.class));
        
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationProducer).sendNotification(messageCaptor.capture());
        
        assertThat(messageCaptor.getValue().getReceiverId()).isEqualTo(100L);
    }
}
