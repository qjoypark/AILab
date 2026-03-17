package com.lab.approval.dto;

import lombok.Data;

import java.util.List;

/**
 * 审批流程定义DTO
 */
@Data
public class ApprovalFlowDefinition {
    
    /**
     * 审批层级列表
     */
    private List<ApprovalLevel> levels;
    
    @Data
    public static class ApprovalLevel {
        /**
         * 层级编号
         */
        private Integer level;
        
        /**
         * 审批人角色
         */
        private String approverRole;
        
        /**
         * 审批人名称
         */
        private String approverName;
        
        /**
         * 是否支持转审
         */
        private Boolean allowTransfer = true;
    }
}
