package com.lab.inventory.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通知消息模板单元测试
 * 
 * 测试消息模板渲染功能：
 * 1. 模板参数替换
 * 2. 不同类型模板渲染
 * 3. 空参数处理
 * 4. 模板查找
 * 
 * **验证需求: 6.4, 18.4**
 */
@DisplayName("通知消息模板测试")
class NotificationTemplateTest {
    
    // ==================== 审批类模板测试 ====================
    
    @Test
    @DisplayName("渲染待审批通知模板 - 应该正确替换所有参数")
    void testRenderApprovalPendingTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.APPROVAL_PENDING;
        Map<String, Object> params = new HashMap<>();
        params.put("applicationType", "危化品领用");
        params.put("applicantName", "张三");
        params.put("applicationNo", "APP202401001");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("您有新的危化品领用申请待审批");
        assertThat(content).isEqualTo("申请人：张三，申请单号：APP202401001，请及时处理。");
    }
    
    @Test
    @DisplayName("渲染审批通过通知模板 - 应该正确替换所有参数")
    void testRenderApprovalApprovedTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.APPROVAL_APPROVED;
        Map<String, Object> params = new HashMap<>();
        params.put("applicationType", "普通领用");
        params.put("applicationNo", "APP202401002");
        params.put("approverName", "李四");
        params.put("opinion", "同意");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("您的普通领用申请已通过");
        assertThat(content).isEqualTo("申请单号：APP202401002，审批人：李四，审批意见：同意");
    }
    
    @Test
    @DisplayName("渲染审批拒绝通知模板 - 应该正确替换所有参数")
    void testRenderApprovalRejectedTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.APPROVAL_REJECTED;
        Map<String, Object> params = new HashMap<>();
        params.put("applicationType", "危化品领用");
        params.put("applicationNo", "APP202401003");
        params.put("approverName", "王五");
        params.put("opinion", "用途说明不清晰");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("您的危化品领用申请已被拒绝");
        assertThat(content).isEqualTo("申请单号：APP202401003，审批人：王五，拒绝原因：用途说明不清晰");
    }
    
    // ==================== 预警类模板测试 ====================
    
    @Test
    @DisplayName("渲染低库存预警模板 - 应该正确替换所有参数")
    void testRenderLowStockAlertTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.ALERT_LOW_STOCK;
        Map<String, Object> params = new HashMap<>();
        params.put("materialName", "无水乙醇");
        params.put("currentStock", "50");
        params.put("unit", "mL");
        params.put("safetyStock", "100");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("无水乙醇库存不足");
        assertThat(content).isEqualTo("当前库存：50mL，安全库存：100mL，请及时补充。");
    }
    
    @Test
    @DisplayName("渲染有效期预警模板 - 应该正确替换所有参数")
    void testRenderExpireSoonAlertTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.ALERT_EXPIRE_SOON;
        Map<String, Object> params = new HashMap<>();
        params.put("materialName", "盐酸");
        params.put("batchNumber", "BATCH20240101");
        params.put("expireDate", "2024-12-31");
        params.put("days", "15");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("盐酸即将过期");
        assertThat(content).isEqualTo("批次号：BATCH20240101，有效期至：2024-12-31，剩余15天，请及时处理。");
    }
    
    @Test
    @DisplayName("渲染危化品账实差异预警模板 - 应该正确替换所有参数")
    void testRenderHazardousDiscrepancyAlertTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.ALERT_HAZARDOUS_DISCREPANCY;
        Map<String, Object> params = new HashMap<>();
        params.put("materialName", "硫酸");
        params.put("bookStock", "1000");
        params.put("unit", "mL");
        params.put("actualStock", "900");
        params.put("diffRate", "10.0");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("硫酸账实差异超标");
        assertThat(content).isEqualTo("账面库存：1000mL，实际库存：900mL，差异率：10.0%，请立即核查。");
    }
    
    @Test
    @DisplayName("渲染异常消耗预警模板 - 应该正确替换所有参数")
    void testRenderAbnormalConsumptionAlertTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.ALERT_ABNORMAL_CONSUMPTION;
        Map<String, Object> params = new HashMap<>();
        params.put("materialName", "丙酮");
        params.put("todayConsumption", "500");
        params.put("unit", "mL");
        params.put("avgConsumption", "100");
        params.put("multiple", "5");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("丙酮消耗异常");
        assertThat(content).isEqualTo("今日消耗：500mL，历史平均：100mL，异常倍数：5倍。");
    }
    
    // ==================== 系统类模板测试 ====================
    
    @Test
    @DisplayName("渲染出库完成通知模板 - 应该正确替换所有参数")
    void testRenderStockOutCompleteTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.SYSTEM_STOCK_OUT_COMPLETE;
        Map<String, Object> params = new HashMap<>();
        params.put("applicationNo", "APP202401004");
        params.put("outOrderNo", "OUT202401001");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("您的领用申请已出库");
        assertThat(content).isEqualTo("申请单号：APP202401004，出库单号：OUT202401001，请及时领取。");
    }
    
    @Test
    @DisplayName("渲染危化品归还提醒模板 - 应该正确替换所有参数")
    void testRenderHazardousReturnReminderTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.SYSTEM_HAZARDOUS_RETURN_REMINDER;
        Map<String, Object> params = new HashMap<>();
        params.put("materialName", "氢氧化钠");
        params.put("receivedQuantity", "200");
        params.put("unit", "g");
        params.put("usageDate", "2024-01-15");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("请及时归还危化品");
        assertThat(content).isEqualTo("药品名称：氢氧化钠，领用数量：200g，领用日期：2024-01-15，请尽快归还。");
    }
    
    @Test
    @DisplayName("渲染系统维护通知模板 - 应该正确替换所有参数")
    void testRenderSystemMaintenanceTemplate() {
        // Given
        NotificationTemplate template = NotificationTemplate.SYSTEM_MAINTENANCE;
        Map<String, Object> params = new HashMap<>();
        params.put("maintenanceTime", "2024-01-20 22:00");
        params.put("duration", "2小时");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("系统维护通知");
        assertThat(content).isEqualTo("系统将于2024-01-20 22:00进行维护，预计持续2小时，请提前做好准备。");
    }
    
    // ==================== 边界情况测试 ====================
    
    @Test
    @DisplayName("渲染模板时参数为空 - 应该保留占位符")
    void testRenderTemplateWithEmptyParams() {
        // Given
        NotificationTemplate template = NotificationTemplate.APPROVAL_PENDING;
        Map<String, Object> params = new HashMap<>();
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).contains("{applicationType}");
        assertThat(content).contains("{applicantName}");
        assertThat(content).contains("{applicationNo}");
    }
    
    @Test
    @DisplayName("渲染模板时参数为null - 应该替换为空字符串")
    void testRenderTemplateWithNullParams() {
        // Given
        NotificationTemplate template = NotificationTemplate.APPROVAL_PENDING;
        Map<String, Object> params = new HashMap<>();
        params.put("applicationType", null);
        params.put("applicantName", null);
        params.put("applicationNo", "APP001");
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("您有新的申请待审批");
        assertThat(content).contains("APP001");
    }
    
    @Test
    @DisplayName("渲染模板时部分参数缺失 - 应该只替换存在的参数")
    void testRenderTemplateWithPartialParams() {
        // Given
        NotificationTemplate template = NotificationTemplate.ALERT_LOW_STOCK;
        Map<String, Object> params = new HashMap<>();
        params.put("materialName", "试剂A");
        params.put("currentStock", "10");
        // 缺少 unit 和 safetyStock
        
        // When
        String title = template.renderTitle(params);
        String content = template.renderContent(params);
        
        // Then
        assertThat(title).isEqualTo("试剂A库存不足");
        assertThat(content).contains("10");
        assertThat(content).contains("{unit}");
        assertThat(content).contains("{safetyStock}");
    }
    
    // ==================== 模板查找测试 ====================
    
    @Test
    @DisplayName("根据代码查找模板 - 应该返回正确的模板")
    void testFindTemplateByCode() {
        // When
        NotificationTemplate template1 = NotificationTemplate.fromCode("APPROVAL_PENDING");
        NotificationTemplate template2 = NotificationTemplate.fromCode("ALERT_LOW_STOCK");
        NotificationTemplate template3 = NotificationTemplate.fromCode("SYSTEM_MAINTENANCE");
        
        // Then
        assertThat(template1).isEqualTo(NotificationTemplate.APPROVAL_PENDING);
        assertThat(template2).isEqualTo(NotificationTemplate.ALERT_LOW_STOCK);
        assertThat(template3).isEqualTo(NotificationTemplate.SYSTEM_MAINTENANCE);
    }
    
    @Test
    @DisplayName("根据不存在的代码查找模板 - 应该返回null")
    void testFindTemplateByInvalidCode() {
        // When
        NotificationTemplate template = NotificationTemplate.fromCode("INVALID_CODE");
        
        // Then
        assertThat(template).isNull();
    }
    
    @Test
    @DisplayName("根据null代码查找模板 - 应该返回null")
    void testFindTemplateByNullCode() {
        // When
        NotificationTemplate template = NotificationTemplate.fromCode(null);
        
        // Then
        assertThat(template).isNull();
    }
    
    // ==================== 参数构建器测试 ====================
    
    @Test
    @DisplayName("使用参数构建器 - 应该创建空Map")
    void testBuildParams() {
        // When
        Map<String, Object> params = NotificationTemplate.buildParams();
        
        // Then
        assertThat(params).isNotNull();
        assertThat(params).isEmpty();
    }
    
    @Test
    @DisplayName("使用参数构建器并添加参数 - 应该正常工作")
    void testBuildParamsAndAdd() {
        // When
        Map<String, Object> params = NotificationTemplate.buildParams();
        params.put("key1", "value1");
        params.put("key2", 123);
        
        // Then
        assertThat(params).hasSize(2);
        assertThat(params.get("key1")).isEqualTo("value1");
        assertThat(params.get("key2")).isEqualTo(123);
    }
    
    // ==================== 模板属性测试 ====================
    
    @Test
    @DisplayName("验证模板属性 - 应该包含所有必需字段")
    void testTemplateProperties() {
        // Given
        NotificationTemplate template = NotificationTemplate.APPROVAL_PENDING;
        
        // Then
        assertThat(template.getCode()).isEqualTo("APPROVAL_PENDING");
        assertThat(template.getName()).isEqualTo("待审批通知");
        assertThat(template.getTitleTemplate()).isNotEmpty();
        assertThat(template.getContentTemplate()).isNotEmpty();
    }
    
    @Test
    @DisplayName("验证所有模板都有唯一代码 - 应该没有重复")
    void testAllTemplatesHaveUniqueCode() {
        // Given
        NotificationTemplate[] templates = NotificationTemplate.values();
        
        // When & Then
        for (int i = 0; i < templates.length; i++) {
            for (int j = i + 1; j < templates.length; j++) {
                assertThat(templates[i].getCode())
                        .isNotEqualTo(templates[j].getCode());
            }
        }
    }
}
