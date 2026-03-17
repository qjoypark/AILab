package com.lab.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通知消息DTO
 * 用于RabbitMQ消息传递
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 接收人ID
     */
    private Long receiverId;
    
    /**
     * 通知类型: 1-审批, 2-预警, 3-系统
     */
    private Integer notificationType;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 内容
     */
    private String content;
    
    /**
     * 业务类型
     */
    private String businessType;
    
    /**
     * 业务ID
     */
    private Long businessId;
    
    /**
     * 推送渠道: 1-站内, 2-微信, 3-短信, 4-邮件
     */
    private Integer pushChannel;
}
