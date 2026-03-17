package com.lab.inventory.mq;

import com.lab.common.config.RabbitMQConfig;
import com.lab.inventory.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知消息生产者
 * 负责将通知消息发送到RabbitMQ队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 发送通知消息到队列
     * 
     * @param message 通知消息
     */
    public void sendNotification(NotificationMessage message) {
        try {
            log.info("发送通知消息到队列: receiverId={}, type={}, title={}", 
                    message.getReceiverId(), message.getNotificationType(), message.getTitle());
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, message);
            
            log.info("通知消息发送成功: receiverId={}", message.getReceiverId());
        } catch (Exception e) {
            log.error("发送通知消息失败: receiverId={}, error={}", 
                    message.getReceiverId(), e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }
}
