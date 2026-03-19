package com.lab.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 领用申请单实体
 */
@Data
@TableName("material_application")
public class MaterialApplication {
    
    /**
     * 申请单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 申请单号
     */
    private String applicationNo;
    
    /**
     * 申请人ID
     */
    private Long applicantId;
    
    /**
     * 申请人姓名
     */
    private String applicantName;
    
    /**
     * 申请部门
     */
    private String applicantDept;
    
    /**
     * 申请类型: 1-普通领用, 2-危化品领用
     */
    private Integer applicationType;
    
    /**
     * 用途说明
     */
    private String usagePurpose;
    
    /**
     * 使用地点
     */
    private String usageLocation;
    
    /**
     * 期望领用日期
     */
    private LocalDate expectedDate;
    
    /**
     * 状态: 1-待审批, 2-审批中, 3-审批通过, 4-审批拒绝, 5-已出库, 6-已完成, 7-已取消
     */
    private Integer status;
    
    /**
     * 审批状态: 0-未审批, 1-审批中, 2-审批通过, 3-审批拒绝
     */
    private Integer approvalStatus;
    
    /**
     * 当前审批人ID
     */
    private Long currentApproverId;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建人
     */
    private Long createdBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新人
     */
    private Long updatedBy;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 删除标记: 0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 当前可审批人姓名（非持久化字段）
     */
    @TableField(exist = false)
    private String currentApproverName;

    /**
     * 当前审批角色编码（非持久化字段）
     */
    @TableField(exist = false)
    private String currentApproverRole;

    /**
     * 当前可审批人ID列表（非持久化字段）
     */
    @TableField(exist = false)
    private List<Long> currentApproverIds;

    /**
     * 当前可审批人姓名列表（非持久化字段）
     */
    @TableField(exist = false)
    private List<String> currentApproverNames;

    /**
     * 当前待审批状态描述（非持久化字段）
     */
    @TableField(exist = false)
    private String currentPendingStatus;
}
