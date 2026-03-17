package com.lab.inventory.mq;

import com.lab.common.config.RabbitMQConfig;
import com.lab.inventory.dto.NotificationMessage;
import com.lab.inventory.entity.Notification;
import com.lab.inventory.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 通知消息消费者
 * 负责处理RabbitMQ队列中的通知消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    
    private final NotificationMapper notificationMapper;
    
    /**
     * 消费通知消息
     * 将消息保存到数据库，并执行推送操作
     * 暂时禁用，因为RabbitMQ队列未创建
     * 
     * @param message 通知消息
     */
    // @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(NotificationMessage message) {
        try {
            log.info("接收到通知消息: receiverId={}, type={}, title={}", 
                    message.getReceiverId(), message.getNotificationType(), message.getTitle());
            
            // 1. 保存通知到数据库
            Notification notification = new Notification();
            notification.setReceiverId(message.getReceiverId());
            notification.setNotificationType(message.getNotificationType());
            notification.setTitle(message.getTitle());
            notification.setContent(message.getContent());
            notification.setBusinessType(message.getBusinessType());
            notification.setBusinessId(message.getBusinessId());
            notification.setPushChannel(message.getPushChannel());
            notification.setIsRead(0); // 未读
            
            notificationMapper.insert(notification);
            log.info("通知已保存到数据库: notificationId={}", notification.getId());
            
            // 2. 根据推送渠道执行推送
            executePush(notification, message);
            
            log.info("通知消息处理完成: receiverId={}", message.getReceiverId());
        } catch (Exception e) {
            log.error("处理通知消息失败: receiverId={}, error={}", 
                    message.getReceiverId(), e.getMessage(), e);
            // 消息处理失败会自动重试（根据RabbitMQ配置）
            throw new RuntimeException("处理通知消息失败", e);
        }
    }
    
    /**
     * 执行推送操作
     * 
     * @param notification 通知记录
     * @param message 通知消息
     */
    private void executePush(Notification notification, NotificationMessage message) {
        Integer pushChannel = message.getPushChannel();
        
        if (pushChannel == null) {
            log.warn("推送渠道为空，跳过推送: notificationId={}", notification.getId());
            return;
        }
        
        switch (pushChannel) {
            case 1: // 站内消息
                pushInApp(notification);
                break;
            case 2: // 微信推送
                pushWeChat(notification);
                break;
            case 3: // 短信推送
                pushSMS(notification);
                break;
            case 4: // 邮件推送
                pushEmail(notification);
                break;
            default:
                log.warn("未知的推送渠道: channel={}, notificationId={}", 
                        pushChannel, notification.getId());
        }
    }
    
    /**
     * 站内消息推送
     * 站内消息已保存到数据库，无需额外操作
     */
    private void pushInApp(Notification notification) {
        log.info("站内消息推送完成: notificationId={}", notification.getId());
        // 站内消息已通过数据库保存完成，前端轮询或WebSocket推送
    }
    
    /**
     * 微信推送
     * TODO: 集成微信公众号或企业微信API
     */
    private void pushWeChat(Notification notification) {
        log.info("微信推送（待实现）: notificationId={}", notification.getId());
        // TODO: 调用微信API发送模板消息
    }
    
    /**
     * 短信推送
     * TODO: 集成短信服务商API
     */
    private void pushSMS(Notification notification) {
        log.info("短信推送（待实现）: notificationId={}", notification.getId());
        // TODO: 调用短信服务商API发送短信
    }
    
    /**
     * 邮件推送
     * TODO: 集成邮件服务
     */
    private void pushEmail(Notification notification) {
        log.info("邮件推送（待实现）: notificationId={}", notification.getId());
        // TODO: 使用JavaMailSender发送邮件
    }
}
