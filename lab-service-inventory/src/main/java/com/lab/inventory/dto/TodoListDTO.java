package com.lab.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 待办事项列表DTO
 * 
 * **验证需求: 18.8, 18.9**
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "待办事项列表")
public class TodoListDTO {
    
    @Schema(description = "待办事项总数")
    private Long total;
    
    @Schema(description = "待审批数量")
    private Long approvalCount;
    
    @Schema(description = "待处理预警数量")
    private Long alertCount;
    
    @Schema(description = "待办事项列表")
    private List<TodoItemDTO> list;
}
