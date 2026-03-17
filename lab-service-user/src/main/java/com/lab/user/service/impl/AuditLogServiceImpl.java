package com.lab.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.entity.AuditLog;
import com.lab.user.mapper.AuditLogMapper;
import com.lab.user.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    
    private final AuditLogMapper auditLogMapper;
    
    @Override
    public void save(AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
            log.debug("Audit log saved: {}", auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
            // 不抛出异常，避免影响主业务流程
        }
    }
    
    @Override
    public IPage<AuditLog> queryPage(Page<AuditLog> page, Long userId, String operationType,
                                      String businessType, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(userId != null, AuditLog::getUserId, userId)
                .eq(operationType != null && !operationType.isEmpty(), 
                    AuditLog::getOperationType, operationType)
                .eq(businessType != null && !businessType.isEmpty(), 
                    AuditLog::getBusinessType, businessType)
                .ge(startTime != null, AuditLog::getOperationTime, startTime)
                .le(endTime != null, AuditLog::getOperationTime, endTime)
                .orderByDesc(AuditLog::getOperationTime);
        
        return auditLogMapper.selectPage(page, wrapper);
    }
}
