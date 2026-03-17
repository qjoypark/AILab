package com.lab.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知DTO
 * 用于通知查询接口响应
 */
@Data
@Schema(description = "通知DTO")
public class NotificationDTO {
    
    @Schema(description = "通知ID")
    private Long id;
    
    @Schema(description = "接收人ID")
    private Long receiverId;
    
    @Schema(description = "通知类型: 1-审批, 2-预警, 3-系统")
    private Integer notificationType;
    
    @Schema(description = "通知类型描述")
    private String notificationTypeDesc;
    
    @Schema(description = "标题")
    private String title;
    
    @Schema(description = "内容")
    private String content;
    
    @Schema(description = "业务类型")
    private String businessType;
    
    @Schema(description = "业务ID")
    private Long businessId;
    
    @Schema(description = "推送渠道: 1-站内, 2-微信, 3-短信, 4-邮件")
    private Integer pushChannel;
    
    @Schema(description = "推送渠道描述")
    private String pushChannelDesc;
    
    @Schema(description = "是否已读: 0-未读, 1-已读")
    private Integer isRead;
    
    @Schema(description = "阅读时间")
    private LocalDateTime readTime;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
