package com.example.cloudnotes.repository;

import com.example.cloudnotes.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知数据访问接口
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 查询用户的所有通知（按创建时间倒序）
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 查询用户的未读通知
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 查询用户的最近通知（限制数量）
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("userId") Long userId, 
                                               org.springframework.data.domain.Pageable pageable);

    /**
     * 统计用户的未读通知数量
     * 使用 @Query 明确返回 Long 类型，避免类型转换问题
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    Long countByUserIdAndIsReadFalse(@Param("userId") Long userId);

    /**
     * 标记通知为已读
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, 
                    @Param("readAt") LocalDateTime readAt);

    /**
     * 标记用户的所有通知为已读
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId, 
                       @Param("readAt") LocalDateTime readAt);

    /**
     * 删除指定日期之前的已读通知
     */
    @Modifying
    @Transactional
    void deleteByUserIdAndIsReadTrueAndCreatedAtBefore(Long userId, LocalDateTime createdAt);

    /**
     * 删除用户的所有已读通知
     */
    @Modifying
    @Transactional
    void deleteByUserIdAndIsReadTrue(Long userId);
}
