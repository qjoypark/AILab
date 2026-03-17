package com.lab.common.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 */
@Configuration
public class RabbitMQConfig {
    
    /**
     * 审计日志队列名称
     */
    public static final String AUDIT_LOG_QUEUE = "audit.log.queue";
    
    /**
     * 通知消息队列名称
     */
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    
    /**
     * 创建审计日志队列
     */
    @Bean
    public Queue auditLogQueue() {
        // durable=true 持久化队列
        return new Queue(AUDIT_LOG_QUEUE, true);
    }
    
    /**
     * 创建通知消息队列
     */
    @Bean
    public Queue notificationQueue() {
        // durable=true 持久化队列
        return new Queue(NOTIFICATION_QUEUE, true);
    }
    
    /**
     * 配置消息转换器，使用JSON格式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
