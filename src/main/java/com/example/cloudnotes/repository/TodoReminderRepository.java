package com.example.cloudnotes.repository;

import com.example.cloudnotes.entity.TodoReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待办任务提醒数据访问接口
 */
public interface TodoReminderRepository extends JpaRepository<TodoReminder, Long> {

    /**
     * 查询任务的所有提醒
     */
    List<TodoReminder> findByTodoId(Long todoId);

    /**
     * 查询任务的激活提醒
     * 注意：重构后提醒时间由 TodoReminderGeneratorService 动态计算
     */
    @Query("SELECT r FROM TodoReminder r " +
           "JOIN FETCH r.todo t " +
           "JOIN FETCH t.user " +
           "WHERE r.isActive = true " +
           "ORDER BY r.createdAt ASC")
    List<TodoReminder> findActiveReminders();

    /**
     * 查询用户的激活提醒
     */
    @Query("SELECT r FROM TodoReminder r " +
           "JOIN FETCH r.todo t " +
           "WHERE t.user.id = :userId " +
           "AND r.isActive = true " +
           "ORDER BY r.createdAt ASC")
    List<TodoReminder> findActiveRemindersByUserId(@Param("userId") Long userId);

    /**
     * 删除任务的所有提醒
     */
    @Modifying
    @Transactional
    void deleteByTodoId(Long todoId);

    /**
     * 取消任务的所有激活提醒
     */
    @Modifying
    @Transactional
    @Query("UPDATE TodoReminder r SET r.isActive = false " +
           "WHERE r.todo.id = :todoId AND r.isActive = true")
    void cancelPendingRemindersByTodoId(@Param("todoId") Long todoId);
}
