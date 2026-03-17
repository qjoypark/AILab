package com.lab.user.integration;

import com.lab.common.entity.AuditLog;
import com.lab.user.mapper.AuditLogMapper;
import com.lab.user.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审计日志集成测试
 * 验证审计日志的保存和查询功能
 */
@SpringBootTest
@ActiveProfiles("test")
public class AuditLogIntegrationTest {
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private AuditLogMapper auditLogMapper;
    
    @Test
    void testSaveAndQueryAuditLog() {
        // 创建审计日志
        AuditLog auditLog = AuditLog.builder()
                .userId(1L)
                .username("admin")
                .realName("管理员")
                .operationType("CREATE")
                .businessType("USER")
                .operationDesc("创建用户")
                .requestMethod("POST")
                .requestUrl("/api/v1/system/users")
                .requestParams("{\"username\":\"test\"}")
                .responseResult("{\"code\":200}")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .operationTime(LocalDateTime.now())
                .executionTime(100)
                .status(1)
                .build();
        
        // 保存审计日志
        auditLogService.save(auditLog);
        
        // 验证保存成功
        assertThat(auditLog.getId()).isNotNull();
        
        // 查询审计日志
        AuditLog savedLog = auditLogMapper.selectById(auditLog.getId());
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getUsername()).isEqualTo("admin");
        assertThat(savedLog.getOperationType()).isEqualTo("CREATE");
        assertThat(savedLog.getBusinessType()).isEqualTo("USER");
        
        // 清理测试数据
        auditLogMapper.deleteById(auditLog.getId());
    }
    
    @Test
    void testAuditLogContainsRequiredFields() {
        // 创建最小审计日志（只包含必需字段）
        AuditLog auditLog = AuditLog.builder()
                .operationType("DELETE")
                .businessType("ROLE")
                .operationDesc("删除角色")
                .operationTime(LocalDateTime.now())
                .status(1)
                .build();
        
        // 保存审计日志
        auditLogService.save(auditLog);
        
        // 验证保存成功
        assertThat(auditLog.getId()).isNotNull();
        
        // 查询并验证必需字段
        AuditLog savedLog = auditLogMapper.selectById(auditLog.getId());
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getOperationType()).isNotNull().isNotEmpty();
        assertThat(savedLog.getBusinessType()).isNotNull().isNotEmpty();
        assertThat(savedLog.getOperationDesc()).isNotNull().isNotEmpty();
        assertThat(savedLog.getOperationTime()).isNotNull();
        
        // 清理测试数据
        auditLogMapper.deleteById(auditLog.getId());
    }
    
    @Test
    void testAuditLogForFailedOperation() {
        // 创建失败操作的审计日志
        AuditLog auditLog = AuditLog.builder()
                .userId(1L)
                .username("admin")
                .operationType("UPDATE")
                .businessType("PERMISSION")
                .operationDesc("更新权限")
                .operationTime(LocalDateTime.now())
                .executionTime(50)
                .status(2) // 失败
                .errorMessage("权限不存在")
                .build();
        
        // 保存审计日志
        auditLogService.save(auditLog);
        
        // 验证保存成功
        assertThat(auditLog.getId()).isNotNull();
        
        // 查询并验证失败状态
        AuditLog savedLog = auditLogMapper.selectById(auditLog.getId());
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getStatus()).isEqualTo(2);
        assertThat(savedLog.getErrorMessage()).isNotNull().isNotEmpty();
        
        // 清理测试数据
        auditLogMapper.deleteById(auditLog.getId());
    }
}
