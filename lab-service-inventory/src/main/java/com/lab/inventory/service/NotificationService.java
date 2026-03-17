package com.lab.inventory.service;

import com.lab.inventory.dto.NotificationPageDTO;
import com.lab.inventory.dto.NotificationQueryDTO;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {
    
    /**
     * 发送预警通知给指定角色的用户
     * 
     * @param roleNames 角色名称列表
     * @param title 通知标题
     * @param content 通知内容
     * @param businessType 业务类型
     * @param businessId 业务ID
     */
    void sendAlertNotification(List<String> roleNames, String title, String content, 
                               String businessType, Long businessId);
    
    /**
     * 发送通知给指定用户
     * 
     * @param userId 用户ID
     * @param title 通知标题
     * @param content 通知内容
     * @param businessType 业务类型
     * @param businessId 业务ID
     */
    void sendNotification(Long userId, String title, String content, 
                         String businessType, Long businessId);
    
    /**
     * 查询通知列表（分页）
     * 
     * @param queryDTO 查询条件
     * @return 通知分页结果
     */
    NotificationPageDTO queryNotifications(NotificationQueryDTO queryDTO);
    
    /**
     * 标记通知为已读
     * 
     * @param id 通知ID
     * @param userId 用户ID（用于权限验证）
     */
    void markAsRead(Long id, Long userId);
    
    /**
     * 标记所有通知为已读
     * 
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);
    
    /**
     * 获取未读消息数量
     * 
     * @param userId 用户ID
     * @return 未读消息数量
     */
    Long getUnreadCount(Long userId);
}
