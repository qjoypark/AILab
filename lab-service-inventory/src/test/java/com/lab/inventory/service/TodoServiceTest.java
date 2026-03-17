package com.lab.inventory.service;

import com.lab.inventory.client.ApprovalClient;
import com.lab.inventory.dto.MaterialApplicationDTO;
import com.lab.inventory.dto.TodoItemDTO;
import com.lab.inventory.dto.TodoListDTO;
import com.lab.inventory.entity.AlertRecord;
import com.lab.inventory.mapper.AlertRecordMapper;
import com.lab.inventory.service.impl.TodoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 待办事项服务测试
 * 
 * **验证需求: 18.8, 18.9**
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("待办事项服务测试")
class TodoServiceTest {
    
    @Mock
    private ApprovalClient approvalClient;
    
    @Mock
    private AlertRecordMapper alertRecordMapper;
    
    @InjectMocks
    private TodoServiceImpl todoService;
    
    private Long testUserId;
    
    @BeforeEach
    void setUp() {
        testUserId = 1L;
    }
    
    @Test
    @DisplayName("获取待办事项列表 - 包含待审批申请和待处理预警")
    void testGetTodoList_WithApprovalsAndAlerts() {
        // Given: 准备待审批申请
        List<MaterialApplicationDTO> pendingApprovals = new ArrayList<>();
        
        MaterialApplicationDTO approval1 = new MaterialApplicationDTO();
        approval1.setId(1L);
        approval1.setApplicationNo("APP001");
        approval1.setApplicantName("张三");
        approval1.setApplicantDept("化学系");
        approval1.setApplicationType(1); // 普通领用
        approval1.setUsagePurpose("实验使用");
        approval1.setExpectedDate(LocalDate.now().plusDays(3));
        approval1.setCreatedTime(LocalDateTime.now().minusHours(2));
        pendingApprovals.add(approval1);
        
        MaterialApplicationDTO approval2 = new MaterialApplicationDTO();
        approval2.setId(2L);
        approval2.setApplicationNo("APP002");
        approval2.setApplicantName("李四");
        approval2.setApplicantDept("生物系");
        approval2.setApplicationType(2); // 危化品领用
        approval2.setUsagePurpose("危化品实验");
        approval2.setExpectedDate(LocalDate.now().plusDays(1));
        approval2.setCreatedTime(LocalDateTime.now().minusHours(1));
        pendingApprovals.add(approval2);
        
        when(approvalClient.getPendingApprovals(testUserId)).thenReturn(pendingApprovals);
        
        // Given: 准备待处理预警
        List<AlertRecord> alertRecords = new ArrayList<>();
        
        AlertRecord alert1 = new AlertRecord();
        alert1.setId(1L);
        alert1.setAlertType(1);
        alert1.setAlertLevel(2); // 警告
        alert1.setAlertTitle("库存不足预警");
        alert1.setAlertContent("试剂A库存低于安全库存");
        alert1.setStatus(1); // 未处理
        alert1.setCreatedTime(LocalDateTime.now().minusHours(3));
        alertRecords.add(alert1);
        
        AlertRecord alert2 = new AlertRecord();
        alert2.setId(2L);
        alert2.setAlertType(2);
        alert2.setAlertLevel(3); // 严重
        alert2.setAlertTitle("有效期预警");
        alert2.setAlertContent("试剂B即将过期");
        alert2.setStatus(1); // 未处理
        alert2.setCreatedTime(LocalDateTime.now().minusHours(4));
        alertRecords.add(alert2);
        
        when(alertRecordMapper.selectList(any())).thenReturn(alertRecords);
        
        // When: 获取待办事项列表
        TodoListDTO result = todoService.getTodoList(testUserId);
        
        // Then: 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getApprovalCount()).isEqualTo(2);
        assertThat(result.getAlertCount()).isEqualTo(2);
        assertThat(result.getList()).hasSize(4);
        
        // 验证排序：按优先级降序
        List<TodoItemDTO> items = result.getList();
        assertThat(items.get(0).getPriority()).isGreaterThanOrEqualTo(items.get(1).getPriority());
        assertThat(items.get(1).getPriority()).isGreaterThanOrEqualTo(items.get(2).getPriority());
        assertThat(items.get(2).getPriority()).isGreaterThanOrEqualTo(items.get(3).getPriority());
    }
    
    @Test
    @DisplayName("获取待办事项列表 - 仅包含待审批申请")
    void testGetTodoList_OnlyApprovals() {
        // Given: 准备待审批申请
        List<MaterialApplicationDTO> pendingApprovals = new ArrayList<>();
        
        MaterialApplicationDTO approval = new MaterialApplicationDTO();
        approval.setId(1L);
        approval.setApplicationNo("APP001");
        approval.setApplicantName("张三");
        approval.setApplicantDept("化学系");
        approval.setApplicationType(1);
        approval.setUsagePurpose("实验使用");
        approval.setExpectedDate(LocalDate.now().plusDays(3));
        approval.setCreatedTime(LocalDateTime.now());
        pendingApprovals.add(approval);
        
        when(approvalClient.getPendingApprovals(testUserId)).thenReturn(pendingApprovals);
        when(alertRecordMapper.selectList(any())).thenReturn(new ArrayList<>());
        
        // When: 获取待办事项列表
        TodoListDTO result = todoService.getTodoList(testUserId);
        
        // Then: 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getApprovalCount()).isEqualTo(1);
        assertThat(result.getAlertCount()).isEqualTo(0);
        assertThat(result.getList()).hasSize(1);
        
        TodoItemDTO item = result.getList().get(0);
        assertThat(item.getType()).isEqualTo("APPROVAL");
        assertThat(item.getTypeDesc()).isEqualTo("待审批");
        assertThat(item.getBusinessId()).isEqualTo(1L);
        assertThat(item.getBusinessNo()).isEqualTo("APP001");
        assertThat(item.getApplicantName()).isEqualTo("张三");
    }
    
    @Test
    @DisplayName("获取待办事项列表 - 仅包含待处理预警")
    void testGetTodoList_OnlyAlerts() {
        // Given: 准备待处理预警
        List<AlertRecord> alertRecords = new ArrayList<>();
        
        AlertRecord alert = new AlertRecord();
        alert.setId(1L);
        alert.setAlertType(1);
        alert.setAlertLevel(3); // 严重
        alert.setAlertTitle("库存不足预警");
        alert.setAlertContent("试剂A库存低于安全库存");
        alert.setStatus(1);
        alert.setCreatedTime(LocalDateTime.now());
        alertRecords.add(alert);
        
        when(approvalClient.getPendingApprovals(testUserId)).thenReturn(new ArrayList<>());
        when(alertRecordMapper.selectList(any())).thenReturn(alertRecords);
        
        // When: 获取待办事项列表
        TodoListDTO result = todoService.getTodoList(testUserId);
        
        // Then: 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getApprovalCount()).isEqualTo(0);
        assertThat(result.getAlertCount()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        
        TodoItemDTO item = result.getList().get(0);
        assertThat(item.getType()).isEqualTo("ALERT");
        assertThat(item.getTypeDesc()).isEqualTo("待处理预警");
        assertThat(item.getBusinessId()).isEqualTo(1L);
        assertThat(item.getPriority()).isEqualTo(4); // 严重预警映射为紧急优先级
    }
    
    @Test
    @DisplayName("获取待办事项列表 - 空列表")
    void testGetTodoList_Empty() {
        // Given: 无待办事项
        when(approvalClient.getPendingApprovals(testUserId)).thenReturn(new ArrayList<>());
        when(alertRecordMapper.selectList(any())).thenReturn(new ArrayList<>());
        
        // When: 获取待办事项列表
        TodoListDTO result = todoService.getTodoList(testUserId);
        
        // Then: 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getApprovalCount()).isEqualTo(0);
        assertThat(result.getAlertCount()).isEqualTo(0);
        assertThat(result.getList()).isEmpty();
    }
    
    @Test
    @DisplayName("待办事项排序 - 按优先级和截止时间")
    void testTodoList_Sorting() {
        // Given: 准备不同优先级和截止时间的待办事项
        List<MaterialApplicationDTO> pendingApprovals = new ArrayList<>();
        
        // 优先级2，截止时间较晚
        MaterialApplicationDTO approval1 = new MaterialApplicationDTO();
        approval1.setId(1L);
        approval1.setApplicationNo("APP001");
        approval1.setApplicantName("张三");
        approval1.setApplicationType(1); // 普通领用，优先级2
        approval1.setUsagePurpose("实验使用");
        approval1.setExpectedDate(LocalDate.now().plusDays(5));
        approval1.setCreatedTime(LocalDateTime.now());
        pendingApprovals.add(approval1);
        
        // 优先级3，截止时间较早
        MaterialApplicationDTO approval2 = new MaterialApplicationDTO();
        approval2.setId(2L);
        approval2.setApplicationNo("APP002");
        approval2.setApplicantName("李四");
        approval2.setApplicationType(2); // 危化品领用，优先级3
        approval2.setUsagePurpose("危化品实验");
        approval2.setExpectedDate(LocalDate.now().plusDays(2));
        approval2.setCreatedTime(LocalDateTime.now());
        pendingApprovals.add(approval2);
        
        // 优先级3，截止时间更早
        MaterialApplicationDTO approval3 = new MaterialApplicationDTO();
        approval3.setId(3L);
        approval3.setApplicationNo("APP003");
        approval3.setApplicantName("王五");
        approval3.setApplicationType(2); // 危化品领用，优先级3
        approval3.setUsagePurpose("危化品实验");
        approval3.setExpectedDate(LocalDate.now().plusDays(1));
        approval3.setCreatedTime(LocalDateTime.now());
        pendingApprovals.add(approval3);
        
        when(approvalClient.getPendingApprovals(testUserId)).thenReturn(pendingApprovals);
        when(alertRecordMapper.selectList(any())).thenReturn(new ArrayList<>());
        
        // When: 获取待办事项列表
        TodoListDTO result = todoService.getTodoList(testUserId);
        
        // Then: 验证排序
        assertThat(result.getList()).hasSize(3);
        
        List<TodoItemDTO> items = result.getList();
        // 第一个应该是优先级3且截止时间最早的
        assertThat(items.get(0).getBusinessNo()).isEqualTo("APP003");
        assertThat(items.get(0).getPriority()).isEqualTo(3);
        
        // 第二个应该是优先级3且截止时间次早的
        assertThat(items.get(1).getBusinessNo()).isEqualTo("APP002");
        assertThat(items.get(1).getPriority()).isEqualTo(3);
        
        // 第三个应该是优先级2的
        assertThat(items.get(2).getBusinessNo()).isEqualTo("APP001");
        assertThat(items.get(2).getPriority()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("危化品领用申请优先级高于普通领用")
    void testHazardousApplicationHigherPriority() {
        // Given: 准备普通领用和危化品领用申请
        List<MaterialApplicationDTO> pendingApprovals = new ArrayList<>();
        
        MaterialApplicationDTO normalApproval = new MaterialApplicationDTO();
        normalApproval.setId(1L);
        normalApproval.setApplicationNo("APP001");
        normalApproval.setApplicantName("张三");
        normalApproval.setApplicationType(1); // 普通领用
        normalApproval.setUsagePurpose("实验使用");
        normalApproval.setCreatedTime(LocalDateTime.now());
        pendingApprovals.add(normalApproval);
        
        MaterialApplicationDTO hazardousApproval = new MaterialApplicationDTO();
        hazardousApproval.setId(2L);
        hazardousApproval.setApplicationNo("APP002");
        hazardousApproval.setApplicantName("李四");
        hazardousApproval.setApplicationType(2); // 危化品领用
        hazardousApproval.setUsagePurpose("危化品实验");
        hazardousApproval.setCreatedTime(LocalDateTime.now());
        pendingApprovals.add(hazardousApproval);
        
        when(approvalClient.getPendingApprovals(testUserId)).thenReturn(pendingApprovals);
        when(alertRecordMapper.selectList(any())).thenReturn(new ArrayList<>());
        
        // When: 获取待办事项列表
        TodoListDTO result = todoService.getTodoList(testUserId);
        
        // Then: 验证危化品领用排在前面
        assertThat(result.getList()).hasSize(2);
        assertThat(result.getList().get(0).getBusinessNo()).isEqualTo("APP002");
        assertThat(result.getList().get(0).getPriority()).isEqualTo(3);
        assertThat(result.getList().get(1).getBusinessNo()).isEqualTo("APP001");
        assertThat(result.getList().get(1).getPriority()).isEqualTo(2);
    }
}
