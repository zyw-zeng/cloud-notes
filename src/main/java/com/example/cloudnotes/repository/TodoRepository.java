package com.example.cloudnotes.repository;

import com.example.cloudnotes.entity.Todo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待办任务数据访问接口
 */
public interface TodoRepository extends JpaRepository<Todo, Long>, JpaSpecificationExecutor<Todo> {

    /**
     * 查询用户的所有任务（按创建时间倒序）
     * 使用 EntityGraph 防止 N+1 查询问题
     * 注意：Hibernate不允许同时EAGER加载多个List集合，只加载tags
     */
    @EntityGraph(attributePaths = {"tags"})
    List<Todo> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 查询用户指定状态的任务
     * 使用 EntityGraph 一次性加载关联数据
     */
    @EntityGraph(attributePaths = {"tags"})
    List<Todo> findByUserIdAndStatusOrderByDueDateAsc(Long userId, Todo.TodoStatus status);

    /**
     * 查询用户的今日待办（截止日期在今天）
     */
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId " +
           "AND t.status IN ('TODO', 'IN_PROGRESS') " +
           "AND t.dueDate >= :dayStart AND t.dueDate < :dayEnd " +
           "ORDER BY t.priority DESC, t.dueDate ASC")
    List<Todo> findTodayTodos(@Param("userId") Long userId,
                              @Param("dayStart") LocalDateTime dayStart,
                              @Param("dayEnd") LocalDateTime dayEnd);

    /**
     * 查询用户的本周待办（截止日期在本周）
     */
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId " +
           "AND t.status IN ('TODO', 'IN_PROGRESS') " +
           "AND t.dueDate BETWEEN :weekStart AND :weekEnd " +
           "ORDER BY t.dueDate ASC")
    List<Todo> findWeekTodos(@Param("userId") Long userId,
                             @Param("weekStart") LocalDateTime weekStart,
                             @Param("weekEnd") LocalDateTime weekEnd);

    /**
     * 查询用户的逾期任务
     */
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId " +
           "AND t.status IN ('TODO', 'IN_PROGRESS') " +
           "AND t.dueDate < :now " +
           "ORDER BY t.dueDate ASC")
    List<Todo> findOverdueTodos(@Param("userId") Long userId,
                                @Param("now") LocalDateTime now);

    /**
     * 查询用户的重要任务
     * 使用 EntityGraph 优化性能
     */
    @EntityGraph(attributePaths = {"tags"})
    List<Todo> findByUserIdAndIsImportantTrueOrderByDueDateAsc(Long userId);

    /**
     * 查询用户的已完成任务
     * 使用 EntityGraph 优化性能
     */
    @EntityGraph(attributePaths = {"tags"})
    List<Todo> findByUserIdAndStatusOrderByCompletedAtDesc(Long userId, Todo.TodoStatus status);

    /**
     * 根据关键词搜索任务（标题或描述）
     */
    @Query("SELECT DISTINCT t FROM Todo t WHERE t.user.id = :userId " +
           "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Todo> searchByKeyword(@Param("userId") Long userId,
                                @Param("keyword") String keyword);

    /**
     * 根据标签搜索任务
     */
    @Query("SELECT DISTINCT t FROM Todo t JOIN t.tags tag " +
           "WHERE t.user.id = :userId AND tag.id IN :tagIds")
    List<Todo> findByUserIdAndTagIds(@Param("userId") Long userId,
                                     @Param("tagIds") List<Long> tagIds);

    /**
     * 统计用户的任务数量（按状态）
     */
    Long countByUserIdAndStatus(Long userId, Todo.TodoStatus status);

    /**
     * 统计用户的逾期任务数量
     */
    @Query("SELECT COUNT(t) FROM Todo t WHERE t.user.id = :userId " +
           "AND t.status IN ('TODO', 'IN_PROGRESS') " +
           "AND t.dueDate < :now")
    Long countOverdueTodos(@Param("userId") Long userId,
                           @Param("now") LocalDateTime now);

    /**
     * 查询所有未完成的任务（用于定时任务）
     * 使用 JOIN FETCH 预加载 reminders 和 user，避免懒加载异常
     */
    @Query("SELECT DISTINCT t FROM Todo t " +
           "LEFT JOIN FETCH t.reminders " +
           "LEFT JOIN FETCH t.user " +
           "WHERE t.status NOT IN ('COMPLETED', 'ARCHIVED')")
    List<Todo> findAllActiveWithReminders();
}
