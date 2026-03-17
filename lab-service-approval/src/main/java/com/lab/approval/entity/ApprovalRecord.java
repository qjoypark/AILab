package com.lab.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批记录实体
 */
@Data
@TableName("approval_record")
public class ApprovalRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 申请单ID
     */
    private Long applicationId;
    
    /**
     * 申请单号
     */
    private String applicationNo;
    
    /**
     * 审批人ID
     */
    private Long approverId;
    
    /**
     * 审批人姓名
     */
    private String approverName;
    
    /**
     * 审批层级
     */
    private Integer approvalLevel;
    
    /**
     * 审批结果: 1-通过, 2-拒绝, 3-转审
     */
    private Integer approvalResult;
    
    /**
     * 审批意见
     */
    private String approvalOpinion;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
