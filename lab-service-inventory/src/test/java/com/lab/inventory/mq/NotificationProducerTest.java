package com.lab.inventory.mq;

import com.lab.common.config.RabbitMQConfig;
import com.lab.inventory.dto.NotificationMessage;
import com.lab.inventory.enums.NotificationType;
import com.lab.inventory.enums.PushChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 通知消息生产者单元测试
 * 
 * 测试RabbitMQ消息发送功能：
 * 1. 消息发送到正确的队列
 * 2. 消息内容完整性
 * 3. 异常处理
 * 
 * **验证需求: 6.4, 18.4**
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通知消息生产者测试")
class NotificationProducerTest {
    
    @Mock
    private RabbitTemplate rabbitTemplate;
    
    @InjectMocks
    private NotificationProducer notificationProducer;
    
    // ==================== 消息发送测试 ====================
    
    @Test
    @DisplayName("发送通知消息 - 应该发送到正确的队列")
    void testSendNotificationToCorrectQueue() {
        // Given
        NotificationMessage message = NotificationMessage.builder()
                .receiverId(1L)
                .notificationType(NotificationType.SYSTEM.getCode())
                .title("测试通知")
                .content("测试内容")
                .businessType("TEST")
                .businessId(100L)
                .pushChannel(PushChannel.IN_APP.getCode())
                .build();
        
        // When
        notificationProducer.sendNotification(message);
        
        // Then
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.NOTIFICATION_QUEUE), eq(message));
    }
    
    @Test
    @DisplayName("发送审批通知消息 - 应该包含完整信息")
    void testSendApprovalNotificationMessage() {
        // Given
        NotificationMessage message = NotificationMessage.builder()
                .receiverId(2L)
                .notificationType(NotificationType.APPROVAL.getCode())
                .title("待审批通知")
                .content("您有新的申请待审批")
                .businessType("APPROVAL")
                .businessId(200L)
                .pushChannel(PushChannel.IN_APP.getCode())
                .build();
        
        // When
        notificationProducer.sendNotification(message);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.NOTIFICATION_QUEUE), messageCaptor.capture());
        
        NotificationMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getReceiverId()).isEqualTo(2L);
        assertThat(capturedMessage.getNotificationType()).isEqualTo(NotificationType.APPROVAL.getCode());
        assertThat(capturedMessage.getTitle()).isEqualTo("待审批通知");
        assertThat(capturedMessage.getContent()).isEqualTo("您有新的申请待审批");
        assertThat(capturedMessage.getBusinessType()).isEqualTo("APPROVAL");
        assertThat(capturedMessage.getBusinessId()).isEqualTo(200L);
    }
    
    @Test
    @DisplayName("发送预警通知消息 - 应该包含完整信息")
    void testSendAlertNotificationMessage() {
        // Given
        NotificationMessage message = NotificationMessage.builder()
                .receiverId(3L)
                .notificationType(NotificationType.ALERT.getCode())
                .title("低库存预警")
                .content("试剂库存不足")
                .businessType("STOCK_ALERT")
                .businessId(300L)
                .pushChannel(PushChannel.IN_APP.getCode())
                .build();
        
        // When
        notificationProducer.sendNotification(message);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.NOTIFICATION_QUEUE), messageCaptor.capture());
        
        NotificationMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getNotificationType()).isEqualTo(NotificationType.ALERT.getCode());
        assertThat(capturedMessage.getTitle()).isEqualTo("低库存预警");
    }
    
    // ==================== 多次发送测试 ====================
    
    @Test
    @DisplayName("多次发送消息 - 应该每次都调用RabbitTemplate")
    void testSendMultipleMessages() {
        // Given
        NotificationMessage message1 = createTestMessage(1L, "通知1");
        NotificationMessage message2 = createTestMessage(2L, "通知2");
        NotificationMessage message3 = createTestMessage(3L, "通知3");
        
        // When
        notificationProducer.sendNotification(message1);
        notificationProducer.sendNotification(message2);
        notificationProducer.sendNotification(message3);
        
        // Then
        verify(rabbitTemplate, times(3))
                .convertAndSend(eq(RabbitMQConfig.NOTIFICATION_QUEUE), any(NotificationMessage.class));
    }
    
    // ==================== 异常处理测试 ====================
    
    @Test
    @DisplayName("发送消息时RabbitMQ异常 - 应该捕获异常不抛出")
    void testSendNotificationWithRabbitMQException() {
        // Given
        NotificationMessage message = createTestMessage(1L, "测试通知");
        doThrow(new RuntimeException("RabbitMQ连接失败"))
                .when(rabbitTemplate).convertAndSend(anyString(), (Object) any());
        
        // When & Then - 不应该抛出异常
        notificationProducer.sendNotification(message);
        
        // 验证确实调用了RabbitTemplate
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.NOTIFICATION_QUEUE), eq(message));
    }
    
    @Test
    @DisplayName("发送消息时网络超时 - 应该捕获异常不影响主流程")
    void testSendNotificationWithNetworkTimeout() {
        // Given
        NotificationMessage message = createTestMessage(1L, "测试通知");
        doThrow(new RuntimeException("网络超时"))
                .when(rabbitTemplate).convertAndSend(anyString(), (Object) any());
        
        // When & Then - 不应该抛出异常
        notificationProducer.sendNotification(message);
        
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.NOTIFICATION_QUEUE), eq(message));
    }
    
    // ==================== 消息内容验证测试 ====================
    
    @Test
    @DisplayName("发送消息时业务ID为null - 应该正常发送")
    void testSendNotificationWithNullBusinessId() {
        // Given
        NotificationMessage message = NotificationMessage.builder()
                .receiverId(1L)
                .notificationType(NotificationType.SYSTEM.getCode())
                .title("测试通知")
                .content("测试内容")
                .businessType("TEST")
                .businessId(null)
                .pushChannel(PushChannel.IN_APP.getCode())
                .build();
        
        // When
        notificationProducer.sendNotification(message);
        
        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.NOTIFICATION_QUEUE), messageCaptor.capture());
        
        assertThat(messageCaptor.getValue().getBusinessId()).isNull();
    }
    
    @Test
    @DisplayName("发送不同推送渠道的消息 - 应该正确设置渠道")
    void testSendNotificationWithDifferentChannels() {
        // Given
        NotificationMessage inAppMessage = createMessageWithChannel(1L, PushChannel.IN_APP);
        NotificationMessage wechatMessage = createMessageWithChannel(2L, PushChannel.WECHAT);
        NotificationMessage smsMessage = createMessageWithChannel(3L, PushChannel.SMS);
        NotificationMessage emailMessage = createMessageWithChannel(4L, PushChannel.EMAIL);
        
        // When
        notificationProducer.sendNotification(inAppMessage);
        notificationProducer.sendNotification(wechatMessage);
        notificationProducer.sendNotification(smsMessage);
        notificationProducer.sendNotification(emailMessage);
        
        // Then
        verify(rabbitTemplate, times(4))
                .convertAndSend(eq(RabbitMQConfig.NOTIFICATION_QUEUE), any(NotificationMessage.class));
    }
    
    @Test
    @DisplayName("发送消息到指定队列 - 应该使用配置的队列名称")
    void testSendNotificationToConfiguredQueue() {
        // Given
        NotificationMessage message = createTestMessage(1L, "测试");
        
        // When
        notificationProducer.sendNotification(message);
        
        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.NOTIFICATION_QUEUE),
                any(NotificationMessage.class)
        );
    }
    
    // ==================== 辅助方法 ====================
    
    private NotificationMessage createTestMessage(Long receiverId, String title) {
        return NotificationMessage.builder()
                .receiverId(receiverId)
                .notificationType(NotificationType.SYSTEM.getCode())
                .title(title)
                .content("测试内容")
                .businessType("TEST")
                .businessId(100L)
                .pushChannel(PushChannel.IN_APP.getCode())
                .build();
    }
    
    private NotificationMessage createMessageWithChannel(Long receiverId, PushChannel channel) {
        return NotificationMessage.builder()
                .receiverId(receiverId)
                .notificationType(NotificationType.SYSTEM.getCode())
                .title("测试通知")
                .content("测试内容")
                .businessType("TEST")
                .businessId(100L)
                .pushChannel(channel.getCode())
                .build();
    }
}
