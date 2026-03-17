package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.dto.MaterialApplicationDTO;
import com.lab.inventory.dto.TodoItemDTO;
import com.lab.inventory.dto.TodoListDTO;
import com.lab.inventory.entity.AlertRecord;
import com.lab.inventory.mapper.AlertRecordMapper;
import com.lab.inventory.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 待办事项服务实现
 * 
 * 聚合待审批申请和待处理预警，提供统一的待办事项列表
 * 按优先级和截止时间排序
 * 
 * **验证需求: 18.8, 18.9**
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {
    
    private final ApprovalClient approvalClient;
    private final AlertRecordMapper alertRecordMapper;
    
    @Override
    public TodoListDTO getTodoList(Long userId) {
        log.info("获取用户待办事项列表: userId={}", userId);
        
        List<TodoItemDTO> todoItems = new ArrayList<>();
        
        // 1. 查询待审批申请
        List<TodoItemDTO> approvalItems = getPendingApprovalItems(userId);
        todoItems.addAll(approvalItems);
        
        // 2. 查询待处理预警
        List<TodoItemDTO> alertItems = getPendingAlertItems(userId);
        todoItems.addAll(alertItems);
        
        // 3. 排序：按优先级降序，相同优先级按截止时间升序
        List<TodoItemDTO> sortedItems = todoItems.stream()
                .sorted(Comparator
                        .comparing(TodoItemDTO::getPriority, Comparator.reverseOrder())
                        .thenComparing(TodoItemDTO::getDeadline, 
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        
        // 4. 构建返回结果
        TodoListDTO result = TodoListDTO.builder()
                .total((long) sortedItems.size())
                .approvalCount((long) approvalItems.size())
                .alertCount((long) alertItems.size())
                .list(sortedItems)
                .build();
        
        log.info("获取用户待办事项列表完成: userId={}, total={}, approvalCount={}, alertCount={}", 
                userId, result.getTotal(), result.getApprovalCount(), result.getAlertCount());
        
        return result;
    }
    
    /**
     * 获取待审批申请待办事项
     * 
     * @param userId 用户ID
     * @return 待审批申请列表
     */
    private List<TodoItemDTO> getPendingApprovalItems(Long userId) {
        log.debug("查询待审批申请: userId={}", userId);
        
        try {
            // 调用审批服务查询当前用户的待审批申请
            List<MaterialApplicationDTO> pendingApprovals = approvalClient.getPendingApprovals(userId);
            
            return pendingApprovals.stream()
                    .map(this::convertApprovalToTodoItem)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询待审批申请失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取待处理预警待办事项
     * 
     * @param userId 用户ID（预警不限定用户，返回所有未处理预警）
     * @return 待处理预警列表
     */
    private List<TodoItemDTO> getPendingAlertItems(Long userId) {
        log.debug("查询待处理预警: userId={}", userId);
        
        try {
            // 查询所有未处理的预警记录
            LambdaQueryWrapper<AlertRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AlertRecord::getStatus, 1); // 1-未处理
            queryWrapper.orderByDesc(AlertRecord::getAlertLevel);
            queryWrapper.orderByDesc(AlertRecord::getAlertTime);
            
            List<AlertRecord> alertRecords = alertRecordMapper.selectList(queryWrapper);
            
            return alertRecords.stream()
                    .map(this::convertAlertToTodoItem)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询待处理预警失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 将申请单转换为待办事项
     * 
     * @param application 申请单
     * @return 待办事项
     */
    private TodoItemDTO convertApprovalToTodoItem(MaterialApplicationDTO application) {
        // 根据申请类型确定优先级
        // 危化品领用（类型2）优先级更高
        Integer priority = application.getApplicationType() == 2 ? 3 : 2;
        
        // 构建标题和内容
        String typeDesc = application.getApplicationType() == 2 ? "危化品领用" : "普通领用";
        String title = String.format("[%s] %s 提交的领用申请", typeDesc, application.getApplicantName());
        String content = String.format("申请单号: %s, 用途: %s", 
                application.getApplicationNo(), 
                application.getUsagePurpose());
        
        // 截止时间：期望领用日期
        LocalDateTime deadline = application.getExpectedDate() != null 
                ? application.getExpectedDate().atStartOfDay() 
                : null;
        
        return TodoItemDTO.builder()
                .type("APPROVAL")
                .typeDesc("待审批")
                .businessId(application.getId())
                .businessNo(application.getApplicationNo())
                .title(title)
                .content(content)
                .priority(priority)
                .priorityDesc(getPriorityDesc(priority))
                .deadline(deadline)
                .createdTime(application.getCreatedTime())
                .applicantName(application.getApplicantName())
                .applicantDept(application.getApplicantDept())
                .build();
    }
    
    /**
     * 将预警记录转换为待办事项
     * 
     * @param alertRecord 预警记录
     * @return 待办事项
     */
    private TodoItemDTO convertAlertToTodoItem(AlertRecord alertRecord) {
        // 预警级别映射到优先级
        // 1-提示 -> 优先级1
        // 2-警告 -> 优先级2
        // 3-严重 -> 优先级4（紧急）
        Integer priority = mapAlertLevelToPriority(alertRecord.getAlertLevel());
        
        // 构建标题和内容
        String title = String.format("[%s] %s", 
                getAlertLevelDesc(alertRecord.getAlertLevel()), 
                alertRecord.getAlertTitle());
        
        return TodoItemDTO.builder()
                .type("ALERT")
                .typeDesc("待处理预警")
                .businessId(alertRecord.getId())
                .businessNo(String.valueOf(alertRecord.getId()))
                .title(title)
                .content(alertRecord.getAlertContent())
                .priority(priority)
                .priorityDesc(getPriorityDesc(priority))
                .deadline(null) // 预警没有明确截止时间
                .createdTime(alertRecord.getCreatedTime())
                .applicantName(null)
                .applicantDept(null)
                .build();
    }
    
    /**
     * 预警级别映射到优先级
     * 
     * @param alertLevel 预警级别
     * @return 优先级
     */
    private Integer mapAlertLevelToPriority(Integer alertLevel) {
        if (alertLevel == null) {
            return 1;
        }
        
        switch (alertLevel) {
            case 1: // 提示
                return 1;
            case 2: // 警告
                return 2;
            case 3: // 严重
                return 4; // 紧急
            default:
                return 1;
        }
    }
    
    /**
     * 获取预警级别描述
     * 
     * @param alertLevel 预警级别
     * @return 描述
     */
    private String getAlertLevelDesc(Integer alertLevel) {
        if (alertLevel == null) {
            return "提示";
        }
        
        switch (alertLevel) {
            case 1:
                return "提示";
            case 2:
                return "警告";
            case 3:
                return "严重";
            default:
                return "提示";
        }
    }
    
    /**
     * 获取优先级描述
     * 
     * @param priority 优先级
     * @return 描述
     */
    private String getPriorityDesc(Integer priority) {
        if (priority == null) {
            return "低";
        }
        
        switch (priority) {
            case 1:
                return "低";
            case 2:
                return "中";
            case 3:
                return "高";
            case 4:
                return "紧急";
            default:
                return "低";
        }
    }
}
