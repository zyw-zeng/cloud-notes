package com.example.cloudnotes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属用户
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 通知标题
     */
    @Column(nullable = false)
    private String title;

    /**
     * 通知内容
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 通知类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /**
     * 关联的资源ID（如任务ID）
     */
    private Long relatedId;

    /**
     * 关联的资源类型（如 TODO）
     */
    private String relatedType;

    /**
     * 是否已读
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 已读时间
     */
    private LocalDateTime readAt;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
    }

    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        TODO_REMINDER,      // 待办任务提醒
        TODO_OVERDUE,       // 任务逾期
        SYSTEM_MESSAGE,     // 系统消息
        COMMENT_REPLY,      // 评论回复
        NOTE_SHARE          // 笔记分享
    }
}
