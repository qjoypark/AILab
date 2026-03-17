package com.lab.inventory.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationType {
    
    /**
     * 审批通知
     */
    APPROVAL(1, "审批通知"),
    
    /**
     * 预警通知
     */
    ALERT(2, "预警通知"),
    
    /**
     * 系统通知
     */
    SYSTEM(3, "系统通知");
    
    private final Integer code;
    private final String description;
    
    public static NotificationType fromCode(Integer code) {
        for (NotificationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
