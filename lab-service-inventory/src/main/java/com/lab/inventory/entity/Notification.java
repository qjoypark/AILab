package com.lab.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知实体
 */
@Data
@TableName("notification")
public class Notification {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long receiverId;
    
    private Integer notificationType;
    
    private String title;
    
    private String content;
    
    private String businessType;
    
    private Long businessId;
    
    private Integer pushChannel;
    
    private Integer isRead;
    
    private LocalDateTime readTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
