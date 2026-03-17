package com.lab.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.common.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
