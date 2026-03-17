package com.lab.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待办事项DTO
 * 
 * 统一的待办事项数据结构，聚合：
 * 1. 待审批申请（来自material_application表）
 * 2. 待处理预警（来自alert_record表）
 * 
 * **验证需求: 18.8, 18.9**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "待办事项")
public class TodoItemDTO {
    
    @Schema(description = "待办事项类型: APPROVAL-待审批, ALERT-待处理预警")
    private String type;
    
    @Schema(description = "待办事项类型描述")
    private String typeDesc;
    
    @Schema(description = "业务ID")
    private Long businessId;
    
    @Schema(description = "业务编号（申请单号或预警编号）")
    private String businessNo;
    
    @Schema(description = "标题")
    private String title;
    
    @Schema(description = "内容描述")
    private String content;
    
    @Schema(description = "优先级: 1-低, 2-中, 3-高, 4-紧急")
    private Integer priority;
    
    @Schema(description = "优先级描述")
    private String priorityDesc;
    
    @Schema(description = "截止时间")
    private LocalDateTime deadline;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
    
    @Schema(description = "申请人/触发人姓名")
    private String applicantName;
    
    @Schema(description = "申请部门")
    private String applicantDept;
}
