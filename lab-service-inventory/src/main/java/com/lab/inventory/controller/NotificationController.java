package com.lab.inventory.controller;

import com.lab.common.result.Result;
import com.lab.inventory.dto.NotificationPageDTO;
import com.lab.inventory.dto.NotificationQueryDTO;
import com.lab.inventory.dto.SendNotificationRequest;
import com.lab.inventory.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "通知管理")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "查询通知列表")
    @GetMapping
    public Result<NotificationPageDTO> queryNotifications(
            @Parameter(description = "接收人ID", required = true) @RequestParam Long receiverId,
            @Parameter(description = "通知类型: 1-审批, 2-预警, 3-系统") @RequestParam(required = false) Integer notificationType,
            @Parameter(description = "是否已读: 0-未读, 1-已读") @RequestParam(required = false) Integer isRead,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size) {

        NotificationQueryDTO queryDTO = new NotificationQueryDTO();
        queryDTO.setReceiverId(receiverId);
        queryDTO.setNotificationType(notificationType);
        queryDTO.setIsRead(isRead);
        queryDTO.setPage(page);
        queryDTO.setSize(size);

        return Result.success(notificationService.queryNotifications(queryDTO));
    }

    @Operation(summary = "标记通知为已读")
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(
            @Parameter(description = "通知ID", required = true) @PathVariable Long id,
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {

        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @Operation(summary = "标记所有通知为已读")
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {

        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(
            @Parameter(description = "通知ID", required = true) @PathVariable Long id,
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {

        notificationService.deleteNotification(id, userId);
        return Result.success();
    }

    @Operation(summary = "获取未读消息数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {

        return Result.success(notificationService.getUnreadCount(userId));
    }

    @Operation(summary = "发送站内消息")
    @PostMapping("/send")
    public Result<Void> sendNotification(@RequestBody SendNotificationRequest request) {
        notificationService.sendNotification(request);
        return Result.success();
    }
}
