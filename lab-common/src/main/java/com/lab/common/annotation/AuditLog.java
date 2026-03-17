package com.lab.common.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * 用于标记需要记录审计日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    
    /**
     * 操作类型
     */
    String operationType();
    
    /**
     * 业务类型
     */
    String businessType();
    
    /**
     * 操作描述
     */
    String description();
}
