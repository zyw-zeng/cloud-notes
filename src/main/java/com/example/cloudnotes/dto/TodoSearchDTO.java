package com.example.cloudnotes.dto;

import com.example.cloudnotes.entity.Todo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待办任务搜索/筛选 DTO
 */
@Data
public class TodoSearchDTO {

    /**
     * 关键词（搜索标题和描述）
     */
    private String keyword;

    /**
     * 任务状态列表
     */
    private List<Todo.TodoStatus> statuses;

    /**
     * 优先级列表
     */
    private List<Todo.TodoPriority> priorities;

    /**
     * 标签 ID 列表
     */
    private List<Long> tagIds;

    /**
     * 只显示重要任务
     */
    private Boolean onlyImportant;

    /**
     * 截止日期范围 - 开始
     */
    private LocalDateTime dueDateStart;

    /**
     * 截止日期范围 - 结束
     */
    private LocalDateTime dueDateEnd;

    /**
     * 是否只显示逾期任务
     */
    private Boolean onlyOverdue;
}
