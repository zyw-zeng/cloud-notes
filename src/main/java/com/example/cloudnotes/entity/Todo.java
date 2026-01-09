package com.example.cloudnotes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待办任务实体
 */
@Entity
@Table(name = "todos", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_priority", columnList = "priority"),
    @Index(name = "idx_due_date", columnList = "due_date"),
    @Index(name = "idx_is_important", columnList = "is_important"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_order_index", columnList = "order_index"),
    @Index(name = "idx_user_status_priority", columnList = "user_id, status, priority"),
    @Index(name = "idx_user_due_date", columnList = "user_id, due_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 任务标题
     */
    @Column(nullable = false)
    private String title;

    /**
     * 任务描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 任务状态：TODO（待办）、IN_PROGRESS（进行中）、COMPLETED（已完成）、ARCHIVED（已归档）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @lombok.Builder.Default
    private TodoStatus status = TodoStatus.TODO;

    /**
     * 优先级：HIGH（高）、MEDIUM（中）、LOW（低）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @lombok.Builder.Default
    private TodoPriority priority = TodoPriority.MEDIUM;

    /**
     * 截止日期
     */
    private LocalDateTime dueDate;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 是否重要（星标）
     */
    @Column(nullable = false)
    @lombok.Builder.Default
    private Boolean isImportant = false;

    /**
     * 排序索引（用于自定义排序）
     */
    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer orderIndex = 0;

    /**
     * 任务类型：ONCE（单次）、DAILY（每日）、WEEKLY（每周）、MONTHLY（每月）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @lombok.Builder.Default
    private TodoType todoType = TodoType.ONCE;

    /**
     * 重复规则（JSON 格式，用于存储复杂的重复规则）
     * 例如：{"daysOfWeek": [1,3,5]} 表示每周一、三、五
     */
    @Column(columnDefinition = "TEXT")
    private String recurrenceRule;

    /**
     * 所属用户
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 关联的标签
     */
    @ManyToMany
    @JoinTable(
            name = "todo_tags",
            joinColumns = @JoinColumn(name = "todo_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    /**
     * 关联的提醒
     */
    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TodoReminder> reminders;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TodoStatus.TODO;
        }
        if (priority == null) {
            priority = TodoPriority.MEDIUM;
        }
        if (isImportant == null) {
            isImportant = false;
        }
        if (orderIndex == null) {
            orderIndex = 0;
        }
        if (todoType == null) {
            todoType = TodoType.ONCE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 任务状态枚举
     */
    public enum TodoStatus {
        TODO,           // 待办
        IN_PROGRESS,    // 进行中
        COMPLETED,      // 已完成
        ARCHIVED        // 已归档
    }

    /**
     * 优先级枚举
     */
    public enum TodoPriority {
        HIGH,    // 高
        MEDIUM,  // 中
        LOW      // 低
    }

    /**
     * 任务类型枚举
     */
    public enum TodoType {
        ONCE,       // 单次任务
        DAILY,      // 每日任务
        WEEKLY,     // 每周任务
        MONTHLY     // 每月任务
    }
}
