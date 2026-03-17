package com.lab.inventory.controller;

import com.lab.inventory.dto.TodoItemDTO;
import com.lab.inventory.dto.TodoListDTO;
import com.lab.inventory.service.TodoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 待办事项控制器测试
 * 
 * **验证需求: 18.8, 18.9**
 */
@WebMvcTest(TodoController.class)
@DisplayName("待办事项控制器测试")
class TodoControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TodoService todoService;
    
    @Test
    @DisplayName("GET /api/v1/todo - 获取待办事项列表成功")
    void testGetTodoList_Success() throws Exception {
        // Given: 准备待办事项数据
        List<TodoItemDTO> todoItems = new ArrayList<>();
        
        TodoItemDTO item1 = TodoItemDTO.builder()
                .type("APPROVAL")
                .typeDesc("待审批")
                .businessId(1L)
                .businessNo("APP001")
                .title("[危化品领用] 张三 提交的领用申请")
                .content("申请单号: APP001, 用途: 实验使用")
                .priority(3)
                .priorityDesc("高")
                .deadline(LocalDateTime.now().plusDays(2))
                .createdTime(LocalDateTime.now())
                .applicantName("张三")
                .applicantDept("化学系")
                .build();
        todoItems.add(item1);
        
        TodoItemDTO item2 = TodoItemDTO.builder()
                .type("ALERT")
                .typeDesc("待处理预警")
                .businessId(1L)
                .businessNo("1")
                .title("[警告] 库存不足预警")
                .content("试剂A库存低于安全库存")
                .priority(2)
                .priorityDesc("中")
                .deadline(null)
                .createdTime(LocalDateTime.now())
                .build();
        todoItems.add(item2);
        
        TodoListDTO todoListDTO = TodoListDTO.builder()
                .total(2L)
                .approvalCount(1L)
                .alertCount(1L)
                .list(todoItems)
                .build();
        
        when(todoService.getTodoList(anyLong())).thenReturn(todoListDTO);
        
        // When & Then: 调用接口并验证响应
        mockMvc.perform(get("/api/v1/todo")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.approvalCount").value(1))
                .andExpect(jsonPath("$.data.alertCount").value(1))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list[0].type").value("APPROVAL"))
                .andExpect(jsonPath("$.data.list[0].businessNo").value("APP001"))
                .andExpect(jsonPath("$.data.list[0].priority").value(3))
                .andExpect(jsonPath("$.data.list[1].type").value("ALERT"))
                .andExpect(jsonPath("$.data.list[1].priority").value(2));
    }
    
    @Test
    @DisplayName("GET /api/v1/todo - 空待办事项列表")
    void testGetTodoList_Empty() throws Exception {
        // Given: 准备空待办事项数据
        TodoListDTO todoListDTO = TodoListDTO.builder()
                .total(0L)
                .approvalCount(0L)
                .alertCount(0L)
                .list(new ArrayList<>())
                .build();
        
        when(todoService.getTodoList(anyLong())).thenReturn(todoListDTO);
        
        // When & Then: 调用接口并验证响应
        mockMvc.perform(get("/api/v1/todo")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.approvalCount").value(0))
                .andExpect(jsonPath("$.data.alertCount").value(0))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list").isEmpty());
    }
    
    @Test
    @DisplayName("GET /api/v1/todo - 缺少userId参数")
    void testGetTodoList_MissingUserId() throws Exception {
        // When & Then: 调用接口并验证响应
        mockMvc.perform(get("/api/v1/todo"))
                .andExpect(status().isBadRequest());
    }
}
