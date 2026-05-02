package com.lab.inventory.service;

import com.lab.inventory.client.UserClient;
import com.lab.inventory.entity.Notification;
import com.lab.inventory.mapper.NotificationMapper;
import com.lab.inventory.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("通知服务测试")
class NotificationServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("应查询角色用户并发送通知")
    void shouldQueryRoleUsersAndSendNotifications() {
        when(userClient.getUserIdsByRoleName("CENTER_ADMIN")).thenReturn(Collections.singletonList(3L));
        when(userClient.getUserIdsByRoleName("SAFETY_ADMIN")).thenReturn(Collections.singletonList(1L));

        notificationService.sendAlertNotification(
                Arrays.asList("CENTER_ADMIN", "SAFETY_ADMIN"),
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );

        verify(userClient).getUserIdsByRoleName("CENTER_ADMIN");
        verify(userClient).getUserIdsByRoleName("SAFETY_ADMIN");
        verify(notificationMapper, times(2)).insert(any(Notification.class));
    }

    @Test
    @DisplayName("应去重用户ID并发送通知")
    void shouldDeduplicateUserIdsAndSendNotifications() {
        when(userClient.getUserIdsByRoleName("CENTER_ADMIN")).thenReturn(Collections.singletonList(1L));
        when(userClient.getUserIdsByRoleName("SAFETY_ADMIN")).thenReturn(Collections.singletonList(1L));

        notificationService.sendAlertNotification(
                Arrays.asList("CENTER_ADMIN", "SAFETY_ADMIN"),
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );

        verify(userClient).getUserIdsByRoleName("CENTER_ADMIN");
        verify(userClient).getUserIdsByRoleName("SAFETY_ADMIN");
        verify(notificationMapper, times(1)).insert(any(Notification.class));
    }

    @Test
    @DisplayName("角色没有用户时不应发送通知")
    void shouldNotSendNotificationWhenNoUsersInRole() {
        when(userClient.getUserIdsByRoleName(anyString())).thenReturn(Collections.emptyList());

        notificationService.sendAlertNotification(
                Collections.singletonList("CENTER_ADMIN"),
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );

        verify(userClient).getUserIdsByRoleName("CENTER_ADMIN");
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    @DisplayName("应向多个用户发送通知")
    void shouldSendNotificationsToMultipleUsers() {
        when(userClient.getUserIdsByRoleName("CENTER_ADMIN")).thenReturn(Arrays.asList(3L, 4L));

        notificationService.sendAlertNotification(
                Collections.singletonList("CENTER_ADMIN"),
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );

        verify(userClient).getUserIdsByRoleName("CENTER_ADMIN");
        verify(notificationMapper, times(2)).insert(any(Notification.class));
    }

    @Test
    @DisplayName("应向单个用户发送通知")
    void shouldSendNotificationToSingleUser() {
        notificationService.sendNotification(
                1L,
                "危化品账实差异预警",
                "差异超过5%",
                "HAZARDOUS_MATERIAL",
                1L
        );

        verify(notificationMapper, times(1)).insert(any(Notification.class));
    }
}
