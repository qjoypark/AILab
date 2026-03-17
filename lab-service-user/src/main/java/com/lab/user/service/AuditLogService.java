package com.lab.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.entity.AuditLog;

import java.time.LocalDateTime;

/**
 * 审计日志服务接口
 */
public interface AuditLogService {
    
    /**
     * 保存审计日志
     */
    void save(AuditLog auditLog);
    
    /**
     * 分页查询审计日志
     */
    IPage<AuditLog> queryPage(Page<AuditLog> page, Long userId, String operationType, 
                               String businessType, LocalDateTime startTime, LocalDateTime endTime);
}
