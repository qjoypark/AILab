package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.client.UserClient;
import com.lab.inventory.dto.NotificationDTO;
import com.lab.inventory.dto.NotificationMessage;
import com.lab.inventory.dto.NotificationPageDTO;
import com.lab.inventory.dto.NotificationQueryDTO;
import com.lab.inventory.entity.Notification;
import com.lab.inventory.enums.NotificationType;
import com.lab.inventory.enums.PushChannel;
import com.lab.inventory.mapper.NotificationMapper;
import com.lab.inventory.mq.NotificationProducer;
import com.lab.inventory.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 * 
 * 实现消息推送服务，支持：
 * 1. 站内消息推送
 * 2. RabbitMQ异步发送消息
 * 3. 消息模板管理
 * 4. 多种消息类型（审批、预警、系统通知）
 * 5. 通知查询、标记已读功能
 * 
 * **验证需求: 6.4, 5.5, 18.5, 18.6, 18.7**
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private final UserClient userClient;
    private final NotificationProducer notificationProducer;
    private final NotificationMapper notificationMapper;
    
    @Override
    public void sendAlertNotification(List<String> roleNames, String title, String content,
                                     String businessType, Long businessId) {
        log.info("发送预警通知: roles={}, title={}", roleNames, title);
        
        // 1. 查询所有角色的用户ID（去重）
        Set<Long> userIds = new HashSet<>();
        for (String roleName : roleNames) {
            List<Long> roleUserIds = userClient.getUserIdsByRoleName(roleName);
            userIds.addAll(roleUserIds);
        }
        
        if (userIds.isEmpty()) {
            log.warn("未找到接收通知的用户: roles={}", roleNames);
            return;
        }
        
        // 2. 向每个用户发送预警通知
        for (Long userId : userIds) {
            sendNotificationWithType(userId, NotificationType.ALERT, title, content, 
                    businessType, businessId, PushChannel.IN_APP);
        }
        
        log.info("预警通知发送完成: roles={}, userCount={}", roleNames, userIds.size());
    }
    
    @Override
    public void sendNotification(Long userId, String title, String content,
                                String businessType, Long businessId) {
        // 默认发送系统通知
        sendNotificationWithType(userId, NotificationType.SYSTEM, title, content, 
                businessType, businessId, PushChannel.IN_APP);
    }
    
    /**
     * 发送指定类型的通知
     * 
     * @param userId 用户ID
     * @param notificationType 通知类型
     * @param title 标题
     * @param content 内容
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param pushChannel 推送渠道
     */
    public void sendNotificationWithType(Long userId, NotificationType notificationType, 
                                        String title, String content,
                                        String businessType, Long businessId,
                                        PushChannel pushChannel) {
        log.info("发送通知: userId={}, type={}, title={}, businessType={}, businessId={}", 
                userId, notificationType.getDescription(), title, businessType, businessId);
        
        // 构建通知消息
        NotificationMessage message = NotificationMessage.builder()
                .receiverId(userId)
                .notificationType(notificationType.getCode())
                .title(title)
                .content(content)
                .businessType(businessType)
                .businessId(businessId)
                .pushChannel(pushChannel.getCode())
                .build();
        
        // 发送到RabbitMQ队列，异步处理
        notificationProducer.sendNotification(message);
        
        log.info("通知已发送到消息队列: userId={}, type={}", userId, notificationType.getDescription());
    }
    
    /**
     * 发送审批通知
     * 
     * @param userId 用户ID
     * @param title 标题
     * @param content 内容
     * @param businessType 业务类型
     * @param businessId 业务ID
     */
    public void sendApprovalNotification(Long userId, String title, String content,
                                        String businessType, Long businessId) {
        sendNotificationWithType(userId, NotificationType.APPROVAL, title, content, 
                businessType, businessId, PushChannel.IN_APP);
    }
    
    @Override
    public NotificationPageDTO queryNotifications(NotificationQueryDTO queryDTO) {
        log.info("查询通知列表: receiverId={}, type={}, isRead={}, page={}, size={}", 
                queryDTO.getReceiverId(), queryDTO.getNotificationType(), 
                queryDTO.getIsRead(), queryDTO.getPage(), queryDTO.getSize());
        
        // 构建查询条件
        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        if (queryDTO.getReceiverId() != null) {
            queryWrapper.eq("receiver_id", queryDTO.getReceiverId());
        }
        if (queryDTO.getNotificationType() != null) {
            queryWrapper.eq("notification_type", queryDTO.getNotificationType());
        }
        if (queryDTO.getIsRead() != null) {
            queryWrapper.eq("is_read", queryDTO.getIsRead());
        }
        queryWrapper.orderByDesc("created_time");
        
        // 分页查询
        Page<Notification> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        Page<Notification> resultPage = notificationMapper.selectPage(page, queryWrapper);
        
        // 转换为DTO
        List<NotificationDTO> notificationDTOList = resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        // 查询未读消息数量
        Long unreadCount = getUnreadCount(queryDTO.getReceiverId());
        
        log.info("查询通知列表完成: total={}, unreadCount={}", resultPage.getTotal(), unreadCount);
        
        return NotificationPageDTO.builder()
                .total(resultPage.getTotal())
                .unreadCount(unreadCount)
                .list(notificationDTOList)
                .build();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long id, Long userId) {
        log.info("标记通知为已读: id={}, userId={}", id, userId);
        
        // 查询通知
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        
        // 验证权限：只能标记自己的通知
        if (!notification.getReceiverId().equals(userId)) {
            throw new BusinessException("无权限操作此通知");
        }
        
        // 如果已读，直接返回
        if (Integer.valueOf(1).equals(notification.getIsRead())) {
            log.info("通知已是已读状态: id={}", id);
            return;
        }
        
        // 更新为已读
        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        int updated = notificationMapper.updateById(notification);
        log.info("标记通知为已读完成: id={}, updated={}", id, updated);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        log.info("标记所有通知为已读: userId={}", userId);
        
        // 查询所有未读通知
        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", userId);
        queryWrapper.eq("is_read", 0);
        
        List<Notification> unreadNotifications = notificationMapper.selectList(queryWrapper);
        
        // 批量更新为已读
        if (!unreadNotifications.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Notification notification : unreadNotifications) {
                notification.setIsRead(1);
                notification.setReadTime(now);
                notificationMapper.updateById(notification);
            }
        }
        
        log.info("标记所有通知为已读完成: userId={}, updated={}", userId, unreadNotifications.size());
    }
    
    @Override
    public Long getUnreadCount(Long userId) {
        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", userId);
        queryWrapper.eq("is_read", 0);
        
        Long count = notificationMapper.selectCount(queryWrapper);
        log.debug("查询未读消息数量: userId={}, count={}", userId, count);
        return count;
    }
    
    /**
     * 转换实体为DTO
     */
    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        BeanUtils.copyProperties(notification, dto);
        
        // 设置类型描述
        NotificationType notificationType = NotificationType.fromCode(notification.getNotificationType());
        if (notificationType != null) {
            dto.setNotificationTypeDesc(notificationType.getDescription());
        }
        
        // 设置渠道描述
        PushChannel pushChannel = PushChannel.fromCode(notification.getPushChannel());
        if (pushChannel != null) {
            dto.setPushChannelDesc(pushChannel.getDescription());
        }
        
        return dto;
    }
}
