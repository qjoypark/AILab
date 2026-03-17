package com.lab.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.approval.entity.ApprovalFlowConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批流程配置Mapper
 */
@Mapper
public interface ApprovalFlowConfigMapper extends BaseMapper<ApprovalFlowConfig> {
}
