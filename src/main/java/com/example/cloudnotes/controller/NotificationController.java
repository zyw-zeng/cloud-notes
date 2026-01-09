package com.example.cloudnotes.controller;

import com.example.cloudnotes.common.Result;
import com.example.cloudnotes.entity.Notification;
import com.example.cloudnotes.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "通知管理", description = "系统通知的查询、标记已读和清理操作")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
        summary = "获取所有通知",
        description = "获取当前用户的所有通知列表"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public Result<List<Notification>> getAllNotifications(Principal principal) {
        List<Notification> notifications = notificationService.getAllNotifications(principal.getName());
        return Result.success(notifications);
    }

    @Operation(
        summary = "获取未读通知",
        description = "获取当前用户的所有未读通知"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/unread")
    public Result<List<Notification>> getUnreadNotifications(Principal principal) {
        List<Notification> notifications = notificationService.getUnreadNotifications(principal.getName());
        return Result.success(notifications);
    }

    @Operation(
        summary = "获取最近通知",
        description = "获取最近的N条通知，按时间倒序排列"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/recent")
    public Result<List<Notification>> getRecentNotifications(
            @Parameter(description = "返回数量限制", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        List<Notification> notifications = notificationService.getRecentNotifications(
                principal.getName(), limit);
        return Result.success(notifications);
    }

    @Operation(
        summary = "获取未读数量",
        description = "获取当前用户的未读通知数量"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount(Principal principal) {
        // 安全处理 Redis 缓存可能返回 Integer 的情况
        // GenericJackson2JsonRedisSerializer 反序列化时会将小数值的 Long 转为 Integer
        Object countObj = notificationService.getUnreadCount(principal.getName());
        
        // 安全转换：支持 Integer、Long 和 null
        Long count;
        if (countObj == null) {
            count = 0L;
        } else if (countObj instanceof Number) {
            count = ((Number) countObj).longValue();
        } else {
            count = 0L;
        }
        
        return Result.success(count);
    }

    @Operation(
        summary = "标记已读",
        description = "将指定的通知标记为已读状态"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "404", description = "通知不存在")
    })
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(
            @Parameter(description = "通知ID", required = true, example = "1")
            @PathVariable Long id,
            Principal principal) {
        notificationService.markAsRead(id, principal.getName());
        return Result.success();
    }

    @Operation(
        summary = "全部标记已读",
        description = "将当前用户的所有未读通知标记为已读"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功")
    })
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(Principal principal) {
        notificationService.markAllAsRead(principal.getName());
        return Result.success();
    }

    @Operation(
        summary = "清理已读通知",
        description = "删除当前用户的所有已读通知"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "清理成功")
    })
    @DeleteMapping("/clean")
    public Result<Void> cleanOldNotifications(Principal principal) {
        notificationService.cleanOldNotifications(principal.getName());
        return Result.success();
    }

    @Operation(
        summary = "测试推送",
        description = "发送一条测试通知，用于验证WebSocket实时推送功能是否正常"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "推送成功")
    })
    @PostMapping("/test-push")
    public Result<Notification> testPush(Principal principal) {
        Notification notification = notificationService.createAndSendNotification(
                principal.getName(),
                "测试通知",
                "这是一条测试通知消息，用于验证 WebSocket 推送功能是否正常工作",
                Notification.NotificationType.SYSTEM_MESSAGE,
                null,
                null
        );
        return Result.success(notification);
    }
}
