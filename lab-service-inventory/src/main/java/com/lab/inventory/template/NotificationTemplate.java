package com.lab.inventory.template;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知消息模板
 * 支持多种消息类型的模板管理
 */
@Getter
@AllArgsConstructor
public enum NotificationTemplate {
    
    // 审批类通知模板
    APPROVAL_PENDING("APPROVAL_PENDING", "待审批通知", 
            "您有新的{applicationType}申请待审批", 
            "申请人：{applicantName}，申请单号：{applicationNo}，请及时处理。"),
    
    APPROVAL_APPROVED("APPROVAL_APPROVED", "审批通过通知", 
            "您的{applicationType}申请已通过", 
            "申请单号：{applicationNo}，审批人：{approverName}，审批意见：{opinion}"),
    
    APPROVAL_REJECTED("APPROVAL_REJECTED", "审批拒绝通知", 
            "您的{applicationType}申请已被拒绝", 
            "申请单号：{applicationNo}，审批人：{approverName}，拒绝原因：{opinion}"),
    
    // 预警类通知模板
    ALERT_LOW_STOCK("ALERT_LOW_STOCK", "低库存预警", 
            "{materialName}库存不足", 
            "当前库存：{currentStock}{unit}，安全库存：{safetyStock}{unit}，请及时补充。"),
    
    ALERT_EXPIRE_SOON("ALERT_EXPIRE_SOON", "有效期预警", 
            "{materialName}即将过期", 
            "批次号：{batchNumber}，有效期至：{expireDate}，剩余{days}天，请及时处理。"),
    
    ALERT_HAZARDOUS_DISCREPANCY("ALERT_HAZARDOUS_DISCREPANCY", "危化品账实差异预警", 
            "{materialName}账实差异超标", 
            "账面库存：{bookStock}{unit}，实际库存：{actualStock}{unit}，差异率：{diffRate}%，请立即核查。"),
    
    ALERT_ABNORMAL_CONSUMPTION("ALERT_ABNORMAL_CONSUMPTION", "异常消耗预警", 
            "{materialName}消耗异常", 
            "今日消耗：{todayConsumption}{unit}，历史平均：{avgConsumption}{unit}，异常倍数：{multiple}倍。"),
    
    // 系统类通知模板
    SYSTEM_STOCK_OUT_COMPLETE("SYSTEM_STOCK_OUT_COMPLETE", "出库完成通知", 
            "您的领用申请已出库", 
            "申请单号：{applicationNo}，出库单号：{outOrderNo}，请及时领取。"),
    
    SYSTEM_HAZARDOUS_RETURN_REMINDER("SYSTEM_HAZARDOUS_RETURN_REMINDER", "危化品归还提醒", 
            "请及时归还危化品", 
            "药品名称：{materialName}，领用数量：{receivedQuantity}{unit}，领用日期：{usageDate}，请尽快归还。"),
    
    SYSTEM_MAINTENANCE("SYSTEM_MAINTENANCE", "系统维护通知", 
            "系统维护通知", 
            "系统将于{maintenanceTime}进行维护，预计持续{duration}，请提前做好准备。");
    
    private final String code;
    private final String name;
    private final String titleTemplate;
    private final String contentTemplate;
    
    /**
     * 根据模板代码获取模板
     */
    public static NotificationTemplate fromCode(String code) {
        for (NotificationTemplate template : values()) {
            if (template.getCode().equals(code)) {
                return template;
            }
        }
        return null;
    }
    
    /**
     * 渲染标题
     */
    public String renderTitle(Map<String, Object> params) {
        return renderTemplate(titleTemplate, params);
    }
    
    /**
     * 渲染内容
     */
    public String renderContent(Map<String, Object> params) {
        return renderTemplate(contentTemplate, params);
    }
    
    /**
     * 渲染模板
     */
    private String renderTemplate(String template, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
    
    /**
     * 构建参数Map的辅助方法
     */
    public static Map<String, Object> buildParams() {
        return new HashMap<>();
    }
}
