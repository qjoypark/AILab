package com.lab.user.listener;

import com.lab.common.config.RabbitMQConfig;
import com.lab.common.entity.AuditLog;
import com.lab.user.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 审计日志消息监听器
 * 从RabbitMQ队列中消费审计日志消息并保存到数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogListener {
    
    private final AuditLogService auditLogService;
    
    /**
     * 监听审计日志队列
     * 暂时禁用，因为RabbitMQ队列未创建
     */
    // @RabbitListener(queues = RabbitMQConfig.AUDIT_LOG_QUEUE)
    public void handleAuditLog(AuditLog auditLog) {
        log.debug("Received audit log message: {}", auditLog);
        try {
            auditLogService.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to process audit log message", e);
            // 可以考虑将失败的消息发送到死信队列
        }
    }
}
