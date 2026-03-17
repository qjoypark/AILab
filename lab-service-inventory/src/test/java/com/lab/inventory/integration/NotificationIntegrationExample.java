package com.lab.inventory.integration;

import com.lab.inventory.dto.NotificationMessage;
import com.lab.inventory.enums.NotificationType;
import com.lab.inventory.enums.PushChannel;
import com.lab.inventory.template.NotificationTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 通知服务集成示例
 * 
 * 演示如何使用通知服务的各个组件：
 * 1. 使用模板生成通知内容
 * 2. 构建通知消息
 * 3. 发送到RabbitMQ队列
 * 4. 消费者处理并保存到数据库
 */
@DisplayName("通知服务集成示例")
class NotificationIntegrationExample {
    
    @Test
    @DisplayName("示例1: 发送审批待处理通知")
    void exampleApprovalPendingNotification() {
        // 1. 准备模板参数
        Map<String, Object> params = NotificationTemplate.buildParams();
        params.put("applicationType", "危化品领用");
        params.put("applicantName", "张三");
        params.put("applicationNo", "APP20240101001");
        
        // 2. 使用模板渲染标题和内容
        String title = NotificationTemplate.APPROVAL_PENDING.renderTitle(params);
        String content = NotificationTemplate.APPROVAL_PENDING.renderContent(params);
        
        // 3. 构建通知消息
        NotificationMessage message = NotificationMessage.builder()
                .receiverId(1L) // 审批人ID
                .notificationType(NotificationType.APPROVAL.getCode())
                .title(title)
                .content(content)
                .businessType("MATERIAL_APPLICATION")
                .businessId(100L)
                .pushChannel(PushChannel.IN_APP.getCode())
                .build();
        
        // 4. 发送到RabbitMQ队列（实际使用时调用 notificationProducer.sendNotification(message)）
        System.out.println("=== 审批待处理通知 ===");
        System.out.println("接收人ID: " + message.getReceiverId());
        System.out.println("通知类型: " + NotificationType.APPROVAL.getDescription());
        System.out.println("标题: " + message.getTitle());
        System.out.println("内容: " + message.getContent());
        System.out.println("推送渠道: " + PushChannel.IN_APP.getDescription());
    }
    
    @Test
    @DisplayName("示例2: 发送低库存预警通知")
    void exampleLowStockAlertNotification() {
        // 1. 准备模板参数
        Map<String, Object> params = NotificationTemplate.buildParams();
        params.put("materialName", "试剂A");
        params.put("currentStock", "5");
        params.put("safetyStock", "10");
        params.put("unit", "瓶");
        
        // 2. 使用模板渲染
        String title = NotificationTemplate.ALERT_LOW_STOCK.renderTitle(params);
        String content = NotificationTemplate.ALERT_LOW_STOCK.renderContent(params);
        
        // 3. 构建通知消息
        NotificationMessage message = NotificationMessage.builder()
                .receiverId(2L) // 中心管理员ID
                .notificationType(NotificationType.ALERT.getCode())
                .title(title)
                .content(content)
                .businessType("STOCK_ALERT")
                .businessId(200L)
                .pushChannel(PushChannel.IN_APP.getCode())
                .build();
        
        System.out.println("\n=== 低库存预警通知 ===");
        System.out.println("接收人ID: " + message.getReceiverId());
        System.out.println("通知类型: " + NotificationType.ALERT.getDescription());
        System.out.println("标题: " + message.getTitle());
        System.out.println("内容: " + message.getContent());
    }
    
    @Test
    @DisplayName("示例3: 发送危化品账实差异预警")
    void exampleHazardousDiscrepancyAlert() {
        // 1. 准备模板参数
        Map<String, Object> params = NotificationTemplate.buildParams();
        params.put("materialName", "硫酸");
        params.put("bookStock", "100");
        params.put("actualStock", "92");
        params.put("unit", "ml");
        params.put("diffRate", "8.0");
        
        // 2. 使用模板渲染
        String title = NotificationTemplate.ALERT_HAZARDOUS_DISCREPANCY.renderTitle(params);
        String content = NotificationTemplate.ALERT_HAZARDOUS_DISCREPANCY.renderContent(params);
        
        // 3. 构建通知消息
        NotificationMessage message = NotificationMessage.builder()
                .receiverId(3L)
                .notificationType(NotificationType.ALERT.getCode())
                .title(title)
                .content(content)
                .businessType("HAZARDOUS_DISCREPANCY")
                .businessId(300L)
                .pushChannel(PushChannel.IN_APP.getCode())
                .build();
        
        System.out.println("\n=== 危化品账实差异预警 ===");
        System.out.println("接收人ID: " + message.getReceiverId());
        System.out.println("标题: " + message.getTitle());
        System.out.println("内容: " + message.getContent());
    }
}
