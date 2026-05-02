package com.lab.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 危化品使用记录实体
 */
@Data
@TableName("hazardous_usage_record")
public class HazardousUsageRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 申请单ID
     */
    private Long applicationId;

    @TableField(exist = false)
    private String applicationNo;
    
    /**
     * 药品ID
     */
    private Long materialId;

    @TableField(exist = false)
    private String materialName;
    
    /**
     * 使用人ID
     */
    private Long userId;
    
    /**
     * 使用人姓名
     */
    private String userName;
    
    /**
     * 领用数量
     */
    private BigDecimal receivedQuantity;
    
    /**
     * 实际使用数量
     */
    private BigDecimal actualUsedQuantity;
    
    /**
     * 归还数量
     */
    private BigDecimal returnedQuantity;
    
    /**
     * 废弃数量
     */
    private BigDecimal wasteQuantity;
    
    /**
     * 使用日期
     */
    private LocalDate usageDate;
    
    /**
     * 归还日期
     */
    private LocalDate returnDate;
    
    /**
     * 使用地点
     */
    private String usageLocation;
    
    /**
     * 使用目的
     */
    private String usagePurpose;
    
    /**
     * 状态: 1-使用中, 2-已归还, 3-已完成
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
