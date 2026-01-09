package com.example.cloudnotes.service;

import com.example.cloudnotes.entity.Notification;
import com.example.cloudnotes.entity.Todo;
import com.example.cloudnotes.entity.TodoReminder;
import com.example.cloudnotes.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Todo 提醒生成服务
 * 根据任务类型和提醒设置动态生成通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoReminderGeneratorService {

    private final TodoRepository todoRepository;
    private final NotificationService notificationService;

    /**
     * 定时检查并发送提醒
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndSendReminders() {
        log.debug("【定时任务】开始检查待办任务提醒");
        
        // 获取所有未完成的任务（使用 JOIN FETCH 避免懒加载异常）
        List<Todo> todos = todoRepository.findAllActiveWithReminders();
        
        LocalDateTime now = LocalDateTime.now();
        int sentCount = 0;
        int overdueCount = 0;
        
        for (Todo todo : todos) {
            // 检查任务是否逾期
            if (isOverdue(todo, now)) {
                sendOverdueNotification(todo);
                overdueCount++;
                continue; // 逾期任务不再发送常规提醒
            }
            
            if (todo.getReminders() == null || todo.getReminders().isEmpty()) {
                continue;
            }
            
            // 计算该任务的提醒时间
            List<LocalDateTime> reminderTimes = calculateReminderTimes(todo, now);
            
            // 为每个提醒时间发送通知
            for (LocalDateTime reminderTime : reminderTimes) {
                // 检查是否到了提醒时间（允许 1 分钟误差）
                if (shouldSendReminder(reminderTime, now)) {
                    for (TodoReminder reminder : todo.getReminders()) {
                        if (reminder.getIsActive()) {
                            sendReminderNotification(todo, reminder, reminderTime);
                            sentCount++;
                        }
                    }
                }
            }
        }
        
        if (sentCount > 0 || overdueCount > 0) {
            log.info("【定时任务】已发送 {} 条任务提醒，{} 条逾期通知", sentCount, overdueCount);
        }
    }

    /**
     * 根据任务类型计算提醒时间
     */
    private List<LocalDateTime> calculateReminderTimes(Todo todo, LocalDateTime now) {
        LocalDateTime dueDate = todo.getDueDate();
        if (dueDate == null) {
            return Collections.emptyList();
        }
        
        List<LocalDateTime> times = new ArrayList<>();
        
        switch (todo.getTodoType()) {
            case ONCE:
                // 单次任务：只提醒一次
                if (dueDate.isAfter(now)) {
                    times.add(dueDate);
                }
                break;
                
            case DAILY:
                // 每日任务：计算今天和明天的提醒时间
                LocalDateTime todayReminder = getTodayReminderTime(dueDate, now);
                if (todayReminder != null && todayReminder.isAfter(now)) {
                    times.add(todayReminder);
                }
                
                LocalDateTime tomorrowReminder = getTodayReminderTime(dueDate, now.plusDays(1));
                if (tomorrowReminder != null) {
                    times.add(tomorrowReminder);
                }
                break;
                
            case WEEKLY:
                // 每周任务：计算本周和下周的提醒时间
                LocalDateTime thisWeekReminder = getWeeklyReminderTime(dueDate, now);
                if (thisWeekReminder != null && thisWeekReminder.isAfter(now)) {
                    times.add(thisWeekReminder);
                }
                
                LocalDateTime nextWeekReminder = getWeeklyReminderTime(dueDate, now.plusWeeks(1));
                if (nextWeekReminder != null) {
                    times.add(nextWeekReminder);
                }
                break;
                
            case MONTHLY:
                // 每月任务：计算本月和下月的提醒时间
                LocalDateTime thisMonthReminder = getMonthlyReminderTime(dueDate, now);
                if (thisMonthReminder != null && thisMonthReminder.isAfter(now)) {
                    times.add(thisMonthReminder);
                }
                
                LocalDateTime nextMonthReminder = getMonthlyReminderTime(dueDate, now.plusMonths(1));
                if (nextMonthReminder != null) {
                    times.add(nextMonthReminder);
                }
                break;
        }
        
        return times;
    }

    /**
     * 获取今天的提醒时间
     */
    private LocalDateTime getTodayReminderTime(LocalDateTime dueDate, LocalDateTime referenceDate) {
        return LocalDateTime.of(
                referenceDate.toLocalDate(),
                dueDate.toLocalTime()
        );
    }

    /**
     * 获取每周任务的提醒时间
     */
    private LocalDateTime getWeeklyReminderTime(LocalDateTime dueDate, LocalDateTime referenceDate) {
        // 获取任务原始的星期几
        int targetDayOfWeek = dueDate.getDayOfWeek().getValue();
        int currentDayOfWeek = referenceDate.getDayOfWeek().getValue();
        
        // 计算距离目标星期几的天数
        int daysUntilTarget = (targetDayOfWeek - currentDayOfWeek + 7) % 7;
        
        return LocalDateTime.of(
                referenceDate.toLocalDate().plusDays(daysUntilTarget),
                dueDate.toLocalTime()
        );
    }

    /**
     * 获取每月任务的提醒时间
     */
    private LocalDateTime getMonthlyReminderTime(LocalDateTime dueDate, LocalDateTime referenceDate) {
        int targetDayOfMonth = dueDate.getDayOfMonth();
        int maxDayOfMonth = referenceDate.toLocalDate().lengthOfMonth();
        
        // 如果目标日期超过当月最大天数，使用当月最后一天
        int actualDayOfMonth = Math.min(targetDayOfMonth, maxDayOfMonth);
        
        return LocalDateTime.of(
                referenceDate.getYear(),
                referenceDate.getMonth(),
                actualDayOfMonth,
                dueDate.getHour(),
                dueDate.getMinute()
        );
    }

    /**
     * 判断是否应该发送提醒
     */
    private boolean shouldSendReminder(LocalDateTime reminderTime, LocalDateTime now) {
        // 允许 1 分钟的误差范围
        return reminderTime.isAfter(now.minusMinutes(1)) 
            && reminderTime.isBefore(now.plusMinutes(1));
    }

    /**
     * 判断任务是否逾期
     */
    private boolean isOverdue(Todo todo, LocalDateTime now) {
        LocalDateTime dueDate = todo.getDueDate();
        if (dueDate == null) {
            return false;
        }
        // 任务截止时间已过且未完成
        return dueDate.isBefore(now) && 
               todo.getStatus() != Todo.TodoStatus.COMPLETED && 
               todo.getStatus() != Todo.TodoStatus.ARCHIVED;
    }

    /**
     * 发送逾期通知
     */
    private void sendOverdueNotification(Todo todo) {
        try {
            // 构建通知内容
            String title = "任务逾期提醒";
            String content = buildOverdueContent(todo);
            
            // 发送通知
            notificationService.createAndSendNotification(
                    todo.getUser().getUsername(),
                    title,
                    content,
                    Notification.NotificationType.TODO_REMINDER,
                    todo.getId(),
                    "TODO"
            );
            
            log.debug("【逾期】已发送逾期通知 - 任务: {}, 用户: {}", todo.getTitle(), todo.getUser().getUsername());
        } catch (Exception e) {
            log.error("【逾期】发送逾期通知失败 - 任务: {}, 错误: {}", todo.getTitle(), e.getMessage(), e);
        }
    }

    /**
     * 构建逾期通知内容
     */
    private String buildOverdueContent(Todo todo) {
        StringBuilder content = new StringBuilder();
        
        // 任务标题
        content.append("任务：").append(todo.getTitle()).append("\n");
        
        // 任务类型
        String typeText = switch (todo.getTodoType()) {
            case ONCE -> "单次任务";
            case DAILY -> "每日任务";
            case WEEKLY -> "每周任务";
            case MONTHLY -> "每月任务";
        };
        content.append("类型：").append(typeText).append("\n");
        
        // 截止时间
        content.append("截止时间：").append(todo.getDueDate()).append("\n");
        
        // 逾期提示
        LocalDateTime now = LocalDateTime.now();
        long overdueDays = java.time.Duration.between(todo.getDueDate(), now).toDays();
        if (overdueDays > 0) {
            content.append("⚠️ 已逾期 ").append(overdueDays).append(" 天");
        } else {
            long overdueHours = java.time.Duration.between(todo.getDueDate(), now).toHours();
            if (overdueHours > 0) {
                content.append("⚠️ 已逾期 ").append(overdueHours).append(" 小时");
            } else {
                long overdueMinutes = java.time.Duration.between(todo.getDueDate(), now).toMinutes();
                content.append("⚠️ 已逾期 ").append(overdueMinutes).append(" 分钟");
            }
        }
        
        return content.toString();
    }

    /**
     * 发送提醒通知
     */
    private void sendReminderNotification(Todo todo, TodoReminder reminder, LocalDateTime reminderTime) {
        try {
            // 计算实际提醒时间（减去提前分钟数）
            LocalDateTime actualReminderTime = reminderTime.minusMinutes(reminder.getAdvanceMinutes());
            
            // 构建通知内容
            String title = "待办任务提醒";
            String content = buildReminderContent(todo, reminder, reminderTime);
            
            // 发送通知
            notificationService.createAndSendNotification(
                    todo.getUser().getUsername(),
                    title,
                    content,
                    Notification.NotificationType.TODO_REMINDER,
                    todo.getId(),
                    "TODO"
            );
            
            log.debug("【提醒】已发送任务提醒 - 任务: {}, 用户: {}", todo.getTitle(), todo.getUser().getUsername());
        } catch (Exception e) {
            log.error("【提醒】发送任务提醒失败 - 任务: {}, 错误: {}", todo.getTitle(), e.getMessage(), e);
        }
    }

    /**
     * 构建提醒内容
     */
    private String buildReminderContent(Todo todo, TodoReminder reminder, LocalDateTime reminderTime) {
        StringBuilder content = new StringBuilder();
        
        // 任务标题
        content.append("任务：").append(todo.getTitle()).append("\n");
        
        // 任务类型
        String typeText = switch (todo.getTodoType()) {
            case ONCE -> "单次任务";
            case DAILY -> "每日任务";
            case WEEKLY -> "每周任务";
            case MONTHLY -> "每月任务";
        };
        content.append("类型：").append(typeText).append("\n");
        
        // 截止时间
        content.append("截止时间：").append(reminderTime).append("\n");
        
        // 提前提醒时间
        if (reminder.getAdvanceMinutes() > 0) {
            content.append("提前 ").append(reminder.getAdvanceMinutes()).append(" 分钟提醒");
        } else {
            content.append("准时提醒");
        }
        
        return content.toString();
    }

    /**
     * 手动为任务生成提醒（用于测试或手动触发）
     */
    public void generateRemindersForTodo(Todo todo) {
        if (todo.getReminders() == null || todo.getReminders().isEmpty()) {
            log.debug("【提醒】任务 {} 没有设置提醒", todo.getTitle());
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> reminderTimes = calculateReminderTimes(todo, now);
        
        log.info("【提醒】为任务 {} 生成了 {} 个提醒时间", todo.getTitle(), reminderTimes.size());
        
        for (LocalDateTime reminderTime : reminderTimes) {
            log.debug("  - 提醒时间: {}", reminderTime);
        }
    }
}
