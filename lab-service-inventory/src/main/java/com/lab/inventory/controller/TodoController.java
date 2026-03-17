package com.lab.inventory.controller;

import com.lab.common.result.Result;
import com.lab.inventory.dto.TodoListDTO;
import com.lab.inventory.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 待办事项控制器
 * 
 * 提供待办事项查询接口，聚合：
 * 1. 待审批申请（当前审批人为该用户的申请）
 * 2. 待处理预警（未处理的预警记录）
 * 
 * 按优先级和截止时间排序
 * 
 * **验证需求: 18.8, 18.9**
 */
@Tag(name = "待办事项管理")
@RestController
@RequestMapping("/api/v1/todo")
@RequiredArgsConstructor
public class TodoController {
    
    private final TodoService todoService;
    
    /**
     * 获取用户的待办事项列表
     * 
     * 聚合待审批申请和待处理预警，按优先级和截止时间排序
     */
    @Operation(summary = "获取待办事项列表", description = "聚合待审批申请和待处理预警，按优先级和截止时间排序")
    @GetMapping
    public Result<TodoListDTO> getTodoList(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
        
        TodoListDTO result = todoService.getTodoList(userId);
        return Result.success(result);
    }
}
