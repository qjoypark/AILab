package com.lab.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.annotation.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 审计日志切面
 * 拦截带有@AuditLog注解的方法，记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 审计日志队列名称
     */
    private static final String AUDIT_LOG_QUEUE = "audit.log.queue";
    
    /**
     * 环绕通知，记录审计日志
     */
    @Around("@annotation(com.lab.common.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLogAnnotation = method.getAnnotation(AuditLog.class);
        
        // 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        
        // 构建审计日志对象
        com.lab.common.entity.AuditLog auditLog = com.lab.common.entity.AuditLog.builder()
                .operationType(auditLogAnnotation.operationType())
                .businessType(auditLogAnnotation.businessType())
                .operationDesc(auditLogAnnotation.description())
                .operationTime(LocalDateTime.now())
                .build();
        
        // 设置请求信息
        if (request != null) {
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setRequestUrl(request.getRequestURI());
            auditLog.setIpAddress(getIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            
            // 获取用户信息（从请求头或Security Context）
            String username = request.getHeader("X-Username");
            String realName = request.getHeader("X-RealName");
            String userIdStr = request.getHeader("X-UserId");
            
            if (username != null) {
                auditLog.setUsername(username);
            }
            if (realName != null) {
                auditLog.setRealName(realName);
            }
            if (userIdStr != null) {
                try {
                    auditLog.setUserId(Long.parseLong(userIdStr));
                } catch (NumberFormatException e) {
                    log.warn("Invalid user ID: {}", userIdStr);
                }
            }
        }
        
        // 记录请求参数
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 过滤掉HttpServletRequest等不需要序列化的参数
                Object[] filteredArgs = Arrays.stream(args)
                        .filter(arg -> !(arg instanceof HttpServletRequest) 
                                && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                        .toArray();
                if (filteredArgs.length > 0) {
                    auditLog.setRequestParams(objectMapper.writeValueAsString(filteredArgs));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to serialize request params", e);
            auditLog.setRequestParams("Failed to serialize");
        }
        
        Object result = null;
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 记录响应结果
            auditLog.setStatus(1); // 成功
            try {
                if (result != null) {
                    String resultJson = objectMapper.writeValueAsString(result);
                    // 限制响应结果长度
                    if (resultJson.length() > 2000) {
                        resultJson = resultJson.substring(0, 2000) + "...";
                    }
                    auditLog.setResponseResult(resultJson);
                }
            } catch (Exception e) {
                log.warn("Failed to serialize response result", e);
                auditLog.setResponseResult("Failed to serialize");
            }
            
        } catch (Throwable e) {
            // 记录异常信息
            auditLog.setStatus(2); // 失败
            auditLog.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            // 计算执行时长
            long executionTime = System.currentTimeMillis() - startTime;
            auditLog.setExecutionTime((int) executionTime);
            
            // 异步发送到RabbitMQ
            try {
                rabbitTemplate.convertAndSend(AUDIT_LOG_QUEUE, auditLog);
                log.debug("Audit log sent to queue: {}", auditLog);
            } catch (Exception e) {
                log.error("Failed to send audit log to queue", e);
            }
        }
        
        return result;
    }
    
    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
