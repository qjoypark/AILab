package com.lab.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批流程配置实体
 */
@Data
@TableName("approval_flow_config")
public class ApprovalFlowConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 流程编码
     */
    private String flowCode;
    
    /**
     * 流程名称
     */
    private String flowName;
    
    /**
     * 业务类型: 1-普通领用, 2-危化品领用
     */
    private Integer businessType;
    
    /**
     * 流程定义(JSON格式)
     */
    private String flowDefinition;
    
    /**
     * 状态: 0-停用, 1-启用
     */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
