package com.lab.inventory.mq;

import com.lab.inventory.dto.NotificationMessage;
import com.lab.inventory.entity.Notification;
import com.lab.inventory.enums.NotificationType;
import com.lab.inventory.enums.PushChannel;
import com.lab.inventory.mapper.NotificationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 通知消息消费者测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("通知消息消费者测试")
class NotificationConsumerTest {
    
    @Mock
    private NotificationMapper notificationMapper;
    
    @InjectMocks
    private NotificationConsumer notificationConsumer;
    
    @Test
    @DisplayName("处理通知消息 - 应该保存到数据库")
    void testHandleNotification() {
        // Given
        NotificationMessage message = NotificationMessage.builder()
                .receiverId(1L)
                .notificationType(NotificationType.ALERT.getCode())
                .title("低库存预警")
                .content("试剂A库存不足")
                .businessType("STOCK_ALERT")
                .businessId(100L)
                .pushChannel(PushChannel.IN_APP.getCode())
                .build();
        
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);
        
        // When
        notificationConsumer.handleNotification(message);
        
        // Then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper, times(1)).insert(captor.capture());
        
        Notification savedNotification = captor.getValue();
        assertThat(savedNotification.getReceiverId()).isEqualTo(1L);
        assertThat(savedNotification.getNotificationType()).isEqualTo(NotificationType.ALERT.getCode());
        assertThat(savedNotification.getTitle()).isEqualTo("低库存预警");
        assertThat(savedNotification.getContent()).isEqualTo("试剂A库存不足");
        assertThat(savedNotification.getIsRead()).isEqualTo(0);
    }
}
