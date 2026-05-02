package com.lab.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.exception.BusinessException;
import com.lab.inventory.client.UserClient;
import com.lab.inventory.dto.NotificationDTO;
import com.lab.inventory.dto.NotificationPageDTO;
import com.lab.inventory.dto.NotificationQueryDTO;
import com.lab.inventory.dto.SendNotificationRequest;
import com.lab.inventory.entity.Notification;
import com.lab.inventory.enums.NotificationType;
import com.lab.inventory.enums.PushChannel;
import com.lab.inventory.mapper.NotificationMapper;
import com.lab.inventory.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final UserClient userClient;
    private final NotificationMapper notificationMapper;

    @Override
    public void sendAlertNotification(List<String> roleNames, String title, String content,
                                      String businessType, Long businessId) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }

        Set<Long> userIds = new HashSet<>();
        for (String roleName : roleNames) {
            List<Long> roleUserIds = userClient.getUserIdsByRoleName(roleName);
            if (roleUserIds != null) {
                userIds.addAll(roleUserIds);
            }
        }

        if (userIds.isEmpty()) {
            log.warn("未找到通知接收人: roles={}", roleNames);
            return;
        }

        for (Long userId : userIds) {
            sendNotificationWithType(userId, NotificationType.ALERT, title, content,
                    businessType, businessId, PushChannel.IN_APP);
        }

        log.info("预警通知发送完成: roles={}, userCount={}", roleNames, userIds.size());
    }

    @Override
    public void sendNotification(Long userId, String title, String content,
                                 String businessType, Long businessId) {
        sendNotificationWithType(userId, NotificationType.SYSTEM, title, content,
                businessType, businessId, PushChannel.IN_APP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(SendNotificationRequest request) {
        if (request == null) {
            throw new BusinessException("通知请求不能为空");
        }
        NotificationType notificationType = NotificationType.fromCode(request.getNotificationType());
        if (notificationType == null) {
            notificationType = NotificationType.SYSTEM;
        }
        PushChannel pushChannel = PushChannel.fromCode(request.getPushChannel());
        if (pushChannel == null) {
            pushChannel = PushChannel.IN_APP;
        }

        sendNotificationWithType(
                request.getReceiverId(),
                notificationType,
                request.getTitle(),
                request.getContent(),
                request.getBusinessType(),
                request.getBusinessId(),
                pushChannel
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendNotificationWithType(Long userId,
                                         NotificationType notificationType,
                                         String title,
                                         String content,
                                         String businessType,
                                         Long businessId,
                                         PushChannel pushChannel) {
        if (userId == null) {
            throw new BusinessException("通知接收人不能为空");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException("通知标题不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("通知内容不能为空");
        }

        NotificationType safeNotificationType = notificationType == null ? NotificationType.SYSTEM : notificationType;
        PushChannel safePushChannel = pushChannel == null ? PushChannel.IN_APP : pushChannel;

        Notification notification = new Notification();
        notification.setReceiverId(userId);
        notification.setNotificationType(safeNotificationType.getCode());
        notification.setTitle(title.trim());
        notification.setContent(content.trim());
        notification.setBusinessType(businessType);
        notification.setBusinessId(businessId);
        notification.setPushChannel(safePushChannel.getCode());
        notification.setIsRead(0);
        notification.setCreatedTime(LocalDateTime.now());
        notificationMapper.insert(notification);

        log.info("通知已创建: notificationId={}, userId={}, type={}, businessType={}, businessId={}",
                notification.getId(), userId, safeNotificationType.getDescription(), businessType, businessId);
    }

    public void sendApprovalNotification(Long userId, String title, String content,
                                         String businessType, Long businessId) {
        sendNotificationWithType(userId, NotificationType.APPROVAL, title, content,
                businessType, businessId, PushChannel.IN_APP);
    }

    @Override
    public NotificationPageDTO queryNotifications(NotificationQueryDTO queryDTO) {
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

        Page<Notification> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        Page<Notification> resultPage = notificationMapper.selectPage(page, queryWrapper);

        List<NotificationDTO> notificationDTOList = resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Long unreadCount = getUnreadCount(queryDTO.getReceiverId());

        return NotificationPageDTO.builder()
                .total(resultPage.getTotal())
                .unreadCount(unreadCount)
                .list(notificationDTOList)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long id, Long userId) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!notification.getReceiverId().equals(userId)) {
            throw new BusinessException("无权限操作此通知");
        }
        if (Integer.valueOf(1).equals(notification.getIsRead())) {
            return;
        }

        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", userId);
        queryWrapper.eq("is_read", 0);

        List<Notification> unreadNotifications = notificationMapper.selectList(queryWrapper);
        if (!unreadNotifications.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Notification notification : unreadNotifications) {
                notification.setIsRead(1);
                notification.setReadTime(now);
                notificationMapper.updateById(notification);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long id, Long userId) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!notification.getReceiverId().equals(userId)) {
            throw new BusinessException("无权限操作此通知");
        }
        notificationMapper.deleteById(id);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", userId);
        queryWrapper.eq("is_read", 0);
        return notificationMapper.selectCount(queryWrapper);
    }

    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        BeanUtils.copyProperties(notification, dto);

        NotificationType notificationType = NotificationType.fromCode(notification.getNotificationType());
        if (notificationType != null) {
            dto.setNotificationTypeDesc(notificationType.getDescription());
        }

        PushChannel pushChannel = PushChannel.fromCode(notification.getPushChannel());
        if (pushChannel != null) {
            dto.setPushChannelDesc(pushChannel.getDescription());
        }

        return dto;
    }
}
