package com.lab.inventory.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 推送渠道枚举
 */
@Getter
@AllArgsConstructor
public enum PushChannel {
    
    /**
     * 站内消息
     */
    IN_APP(1, "站内消息"),
    
    /**
     * 微信推送
     */
    WECHAT(2, "微信推送"),
    
    /**
     * 短信推送
     */
    SMS(3, "短信推送"),
    
    /**
     * 邮件推送
     */
    EMAIL(4, "邮件推送");
    
    private final Integer code;
    private final String description;
    
    public static PushChannel fromCode(Integer code) {
        for (PushChannel channel : values()) {
            if (channel.getCode().equals(code)) {
                return channel;
            }
        }
        return null;
    }
}
