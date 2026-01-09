package com.example.cloudnotes.dto;

import com.example.cloudnotes.entity.Todo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待办任务 DTO（用于创建和更新）
 */
@Data
public class TodoDTO {

    /**
     * 任务标题（必填）
     */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 255, message = "任务标题长度不能超过255个字符")
    private String title;

    /**
     * 任务描述
     */
    @Size(max = 5000, message = "任务描述长度不能超过5000个字符")
    private String description;

    /**
     * 任务状态
     */
    private Todo.TodoStatus status;

    /**
     * 优先级
     */
    private Todo.TodoPriority priority;

    /**
     * 截止日期
     */
    private LocalDateTime dueDate;

    /**
     * 是否重要
     */
    private Boolean isImportant;

    /**
     * 排序索引
     */
    private Integer orderIndex;

    /**
     * 任务类型：ONCE（单次）、DAILY（每日）、WEEKLY（每周）、MONTHLY（每月）
     */
    private Todo.TodoType todoType;

    /**
     * 重复规则（JSON 格式）
     * 例如：{"daysOfWeek": [1,3,5]} 表示每周一、三、五
     */
    private String recurrenceRule;

    /**
     * 标签 ID 列表
     */
    private List<Long> tagIds;

    /**
     * 提醒设置列表
     */
    private List<ReminderDTO> reminders;
}
