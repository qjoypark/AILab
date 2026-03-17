package com.lab.approval.dto;

import lombok.Data;

/**
 * 审批上下文DTO - 用于自动分配审批人
 */
@Data
public class ApprovalContext {
    
    /**
     * 申请人ID
     */
    private Long applicantId;
    
    /**
     * 申请单号
     */
    private String applicationNo;
    
    /**
     * 申请单ID
     */
    private Long applicationId;
    
    /**
     * 业务类型
     */
    private Integer businessType;
    
    /**
     * 申请人部门
     */
    private String applicantDept;
    
    /**
     * 申请类型: 1-普通领用, 2-危化品领用
     */
    private Integer applicationType;
    
    /**
     * 药品类型: 1-耗材, 2-试剂, 3-危化品
     */
    private Integer materialType;
    
    /**
     * 是否包含管控物品
     */
    private Boolean hasControlledMaterial;
}
