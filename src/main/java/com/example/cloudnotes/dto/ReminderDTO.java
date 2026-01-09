package com.example.cloudnotes.dto;

import com.example.cloudnotes.entity.TodoReminder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒设置 DTO
 * 简化后只包含提醒规则，不包含任务重复类型
 */
@Data
public class ReminderDTO {

    /**
     * 提前多少分钟提醒（0 表示准时提醒）
     * 相对于任务的截止时间 (dueDate)
     */
    private Integer advanceMinutes;

    /**
     * 提醒方式：IN_APP（站内通知）、EMAIL（邮件）、WEBSOCKET（实时推送）
     */
    private TodoReminder.NotifyMethod notifyMethod;

    /**
     * 是否激活（可用于暂停提醒）
     */
    private Boolean isActive;
}
