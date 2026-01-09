package com.example.cloudnotes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待办任务提醒实体
 */
@Entity
@Table(name = "todo_reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的任务
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private Todo todo;

    /**
     * 提前多少分钟提醒（0表示准时提醒）
     * 相对于任务的截止时间 (dueDate)
     */
    @Column(nullable = false)
    private Integer advanceMinutes = 0;

    /**
     * 提醒方式：IN_APP（站内通知）、EMAIL（邮件）、WEBSOCKET（实时推送）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotifyMethod notifyMethod = NotifyMethod.IN_APP;

    /**
     * 是否激活（可用于暂停提醒）
     */
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (advanceMinutes == null) {
            advanceMinutes = 0;
        }
        if (notifyMethod == null) {
            notifyMethod = NotifyMethod.IN_APP;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    /**
     * 通知方式枚举
     */
    public enum NotifyMethod {
        IN_APP,     // 站内通知
        EMAIL,      // 邮件
        WEBSOCKET   // WebSocket 实时推送
    }
}
