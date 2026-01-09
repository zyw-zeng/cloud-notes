package com.example.cloudnotes.service;

import com.example.cloudnotes.config.CacheConfig;
import com.example.cloudnotes.entity.Notification;
import com.example.cloudnotes.entity.User;
import com.example.cloudnotes.repository.NotificationRepository;
import com.example.cloudnotes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 创建并发送通知
     * 创建后清除用户的通知缓存
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_NOTIFICATIONS_CACHE, key = "#username"),
        @CacheEvict(value = CacheConfig.NOTIFICATION_COUNT_CACHE, key = "#username")
    })
    public Notification createAndSendNotification(String username, 
                                                  String title,
                                                  String content,
                                                  Notification.NotificationType type,
                                                  Long relatedId,
                                                  String relatedType) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        // 创建通知
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .type(type)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        
        // 通过 WebSocket 实时推送
        sendWebSocketNotification(username, notification);
        
        log.info("【通知】已创建并发送通知 - 用户: {}, 标题: {}", username, title);
        return notification;
    }

    /**
     * 通过 WebSocket 发送通知
     */
    private void sendWebSocketNotification(String username, Notification notification) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("id", notification.getId());
            message.put("title", notification.getTitle());
            message.put("content", notification.getContent());
            message.put("type", notification.getType());
            message.put("relatedId", notification.getRelatedId());
            message.put("relatedType", notification.getRelatedType());
            message.put("createdAt", notification.getCreatedAt());
            
            // 发送到用户的私有队列
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/notifications",
                    message
            );
            
            log.debug("【WebSocket】已推送通知到用户 {} 的队列", username);
        } catch (Exception e) {
            log.error("【WebSocket】推送通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取用户的所有通知
     */
    public List<Notification> getAllNotifications(String username) {
        User user = getUserByUsername(username);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * 获取用户的未读通知（使用缓存）
     */
    @Cacheable(value = CacheConfig.USER_NOTIFICATIONS_CACHE, key = "#username + '_unread'")
    public List<Notification> getUnreadNotifications(String username) {
        User user = getUserByUsername(username);
        log.debug("【缓存】查询数据库 - 用户 {} 的未读通知", username);
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
    }

    /**
     * 获取用户的最近通知（限制数量，使用缓存）
     */
    @Cacheable(value = CacheConfig.USER_NOTIFICATIONS_CACHE, key = "#username + '_recent_' + #limit")
    public List<Notification> getRecentNotifications(String username, int limit) {
        User user = getUserByUsername(username);
        log.debug("【缓存】查询数据库 - 用户 {} 的最近 {} 条通知", username, limit);
        return notificationRepository.findRecentNotifications(
                user.getId(), 
                PageRequest.of(0, limit)
        );
    }

    /**
     * 获取未读通知数量（使用缓存）
     * 注意：返回 Object 类型以避免 Redis 缓存反序列化时的类型转换问题
     * 实际返回的是 Long 或 Integer，调用方需要安全转换为 Long
     */
    @Cacheable(value = CacheConfig.NOTIFICATION_COUNT_CACHE, key = "#username")
    public Object getUnreadCount(String username) {
        User user = getUserByUsername(username);
        log.debug("【缓存】查询数据库 - 用户 {} 的未读通知数量", username);
        Long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        // 返回 Long 类型，但缓存可能将其反序列化为 Integer
        return count != null ? count : 0L;
    }

    /**
     * 标记通知为已读（清除缓存）
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_NOTIFICATIONS_CACHE, key = "#username + '_unread'"),
        @CacheEvict(value = CacheConfig.NOTIFICATION_COUNT_CACHE, key = "#username")
    })
    public void markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        if (!notification.getUser().getUsername().equals(username)) {
            throw new RuntimeException("无权操作他人的通知");
        }

        notificationRepository.markAsRead(notificationId, LocalDateTime.now());
        log.info("【通知】用户 {} 标记通知 {} 为已读", username, notificationId);
    }

    /**
     * 标记所有通知为已读（清除所有缓存）
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_NOTIFICATIONS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.NOTIFICATION_COUNT_CACHE, key = "#username")
    })
    public void markAllAsRead(String username) {
        User user = getUserByUsername(username);
        notificationRepository.markAllAsRead(user.getId(), LocalDateTime.now());
        log.info("【通知】用户 {} 标记所有通知为已读", username);
    }

    /**
     * 清理所有已读通知（清除缓存）
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_NOTIFICATIONS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.NOTIFICATION_COUNT_CACHE, key = "#username")
    })
    public void cleanOldNotifications(String username) {
        User user = getUserByUsername(username);
        notificationRepository.deleteByUserIdAndIsReadTrue(user.getId());
        log.info("【通知】已清理用户 {} 的所有已读通知", username);
    }

    /**
     * 根据用户名获取用户
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));
    }
}
