package com.lab.inventory.service;

import com.lab.inventory.dto.TodoListDTO;

/**
 * 待办事项服务接口
 * 
 * 聚合待审批申请和待处理预警，提供统一的待办事项列表
 * 
 * **验证需求: 18.8, 18.9**
 */
public interface TodoService {
    
    /**
     * 获取用户的待办事项列表
     * 
     * 聚合以下待办事项：
     * 1. 待审批申请（当前审批人为该用户的申请）
     * 2. 待处理预警（未处理的预警记录）
     * 
     * 按优先级和截止时间排序：
     * - 优先级高的排在前面
     * - 相同优先级按截止时间升序（最早截止的排在前面）
     * - 无截止时间的排在最后
     * 
     * @param userId 用户ID
     * @return 待办事项列表
     */
    TodoListDTO getTodoList(Long userId);
}
