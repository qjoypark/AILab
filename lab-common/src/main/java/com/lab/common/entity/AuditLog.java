package com.lab.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_log")
public class AuditLog {
    
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 操作人ID
     */
    private Long userId;
    
    /**
     * 操作人用户名
     */
    private String username;
    
    /**
     * 操作人姓名
     */
    private String realName;
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 业务类型
     */
    private String businessType;
    
    /**
     * 业务ID
     */
    private Long businessId;
    
    /**
     * 操作描述
     */
    private String operationDesc;
    
    /**
     * 请求方法
     */
    private String requestMethod;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * 请求参数
     */
    private String requestParams;
    
    /**
     * 响应结果
     */
    private String responseResult;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
    
    /**
     * 执行时长(ms)
     */
    private Integer executionTime;
    
    /**
     * 状态:1-成功,2-失败
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
