package com.example.cloudnotes.service;

import com.example.cloudnotes.dto.ReminderDTO;
import com.example.cloudnotes.dto.TodoDTO;
import com.example.cloudnotes.dto.TodoSearchDTO;
import com.example.cloudnotes.entity.Tag;
import com.example.cloudnotes.entity.Todo;
import com.example.cloudnotes.entity.TodoReminder;
import com.example.cloudnotes.entity.User;
import com.example.cloudnotes.exception.TodoNotFoundException;
import com.example.cloudnotes.exception.UnauthorizedAccessException;
import com.example.cloudnotes.repository.TagRepository;
import com.example.cloudnotes.repository.TodoReminderRepository;
import com.example.cloudnotes.repository.TodoRepository;
import com.example.cloudnotes.repository.UserRepository;
import com.example.cloudnotes.specification.TodoSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 待办任务业务逻辑服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final TodoReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    /**
     * 创建待办任务
     */
    @Transactional
    public Todo createTodo(TodoDTO dto, String username) {
        // 获取用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        // 创建任务
        Todo todo = Todo.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus() != null ? dto.getStatus() : Todo.TodoStatus.TODO)
                .priority(dto.getPriority() != null ? dto.getPriority() : Todo.TodoPriority.MEDIUM)
                .dueDate(dto.getDueDate())
                .isImportant(dto.getIsImportant() != null ? dto.getIsImportant() : false)
                .orderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0)
                .todoType(dto.getTodoType() != null ? dto.getTodoType() : Todo.TodoType.ONCE)
                .recurrenceRule(dto.getRecurrenceRule())
                .user(user)
                .build();

        // 处理标签
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(dto.getTagIds());
            todo.setTags(tags);
        }

        // 保存任务
        todo = todoRepository.save(todo);

        // 处理提醒
        if (dto.getReminders() != null && !dto.getReminders().isEmpty()) {
            List<TodoReminder> reminders = createReminders(todo, dto.getReminders());
            todo.setReminders(reminders);
        }

        log.info("【待办任务】用户 {} 创建任务: {}", username, todo.getTitle());
        return todo;
    }

    /**
     * 更新待办任务
     */
    @Transactional
    public Todo updateTodo(Long id, TodoDTO dto, String username) {
        Todo todo = getTodoAndCheckPermission(id, username);

        // 更新基本信息
        if (dto.getTitle() != null) {
            todo.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            todo.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            todo.setStatus(dto.getStatus());
            // 如果标记为完成，设置完成时间
            if (dto.getStatus() == Todo.TodoStatus.COMPLETED && todo.getCompletedAt() == null) {
                todo.setCompletedAt(LocalDateTime.now());
            }
        }
        if (dto.getPriority() != null) {
            todo.setPriority(dto.getPriority());
        }
        if (dto.getDueDate() != null) {
            todo.setDueDate(dto.getDueDate());
        }
        if (dto.getIsImportant() != null) {
            todo.setIsImportant(dto.getIsImportant());
        }
        if (dto.getOrderIndex() != null) {
            todo.setOrderIndex(dto.getOrderIndex());
        }
        if (dto.getTodoType() != null) {
            todo.setTodoType(dto.getTodoType());
        }
        if (dto.getRecurrenceRule() != null) {
            todo.setRecurrenceRule(dto.getRecurrenceRule());
        }

        // 更新标签
        if (dto.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(dto.getTagIds());
            todo.setTags(tags);
        }

        // 更新提醒
        if (dto.getReminders() != null) {
            // 删除旧提醒
            reminderRepository.deleteByTodoId(id);
            // 创建新提醒
            List<TodoReminder> reminders = createReminders(todo, dto.getReminders());
            todo.setReminders(reminders);
        }

        log.info("【待办任务】用户 {} 更新任务: {}", username, todo.getTitle());
        return todoRepository.save(todo);
    }

    /**
     * 删除待办任务
     */
    @Transactional
    public void deleteTodo(Long id, String username) {
        Todo todo = getTodoAndCheckPermission(id, username);
        todoRepository.delete(todo);
        log.info("【待办任务】用户 {} 删除任务: {}", username, todo.getTitle());
    }

    /**
     * 标记任务为完成
     */
    @Transactional
    public Todo completeTodo(Long id, String username) {
        Todo todo = getTodoAndCheckPermission(id, username);
        
        // 先取消未发送的提醒
        reminderRepository.cancelPendingRemindersByTodoId(id);
        
        // 然后更新任务状态
        todo.setStatus(Todo.TodoStatus.COMPLETED);
        todo.setCompletedAt(LocalDateTime.now());
        Todo savedTodo = todoRepository.save(todo);
        
        log.info("【待办任务】用户 {} 完成任务: {}", username, todo.getTitle());
        return savedTodo;
    }

    /**
     * 获取任务详情
     */
    public Todo getTodo(Long id, String username) {
        return getTodoAndCheckPermission(id, username);
    }

    /**
     * 获取用户的所有任务
     */
    public List<Todo> getAllTodos(String username) {
        User user = getUserByUsername(username);
        return todoRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * 获取今日待办
     */
    public List<Todo> getTodayTodos(String username) {
        User user = getUserByUsername(username);
        LocalDateTime dayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime dayEnd = dayStart.plusDays(1);
        return todoRepository.findTodayTodos(user.getId(), dayStart, dayEnd);
    }

    /**
     * 获取本周待办
     */
    public List<Todo> getWeekTodos(String username) {
        User user = getUserByUsername(username);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime weekEnd = weekStart.plusDays(7);
        return todoRepository.findWeekTodos(user.getId(), weekStart, weekEnd);
    }

    /**
     * 获取逾期任务
     */
    public List<Todo> getOverdueTodos(String username) {
        User user = getUserByUsername(username);
        return todoRepository.findOverdueTodos(user.getId(), LocalDateTime.now());
    }

    /**
     * 获取重要任务
     */
    public List<Todo> getImportantTodos(String username) {
        User user = getUserByUsername(username);
        return todoRepository.findByUserIdAndIsImportantTrueOrderByDueDateAsc(user.getId());
    }

    /**
     * 获取已完成任务
     */
    public List<Todo> getCompletedTodos(String username) {
        User user = getUserByUsername(username);
        return todoRepository.findByUserIdAndStatusOrderByCompletedAtDesc(
                user.getId(), Todo.TodoStatus.COMPLETED);
    }

    /**
     * 搜索任务（使用 Specification 动态查询）
     */
    public List<Todo> searchTodos(TodoSearchDTO searchDTO, String username) {
        User user = getUserByUsername(username);
        
        // 使用 Specification 在数据库层进行筛选
        Specification<Todo> spec = TodoSpecification.buildSearchSpec(user.getId(), searchDTO);
        
        // 如果有标签筛选，需要额外处理
        if (searchDTO.getTagIds() != null && !searchDTO.getTagIds().isEmpty()) {
            return todoRepository.findByUserIdAndTagIds(user.getId(), searchDTO.getTagIds());
        }
        
        return todoRepository.findAll(spec);
    }

    /**
     * 创建提醒列表
     * 简化后的提醒只包含提醒规则，不包含任务重复类型
     */
    private List<TodoReminder> createReminders(Todo todo, List<ReminderDTO> reminderDTOs) {
        List<TodoReminder> reminders = new ArrayList<>();
        
        for (ReminderDTO dto : reminderDTOs) {
            TodoReminder reminder = TodoReminder.builder()
                    .todo(todo)
                    .advanceMinutes(dto.getAdvanceMinutes() != null ? dto.getAdvanceMinutes() : 0)
                    .notifyMethod(dto.getNotifyMethod() != null ? dto.getNotifyMethod() : TodoReminder.NotifyMethod.IN_APP)
                    .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                    .build();
            reminders.add(reminderRepository.save(reminder));
        }
        
        return reminders;
    }

    /**
     * 获取任务并验证权限
     */
    private Todo getTodoAndCheckPermission(Long id, String username) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));

        if (!todo.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("无权操作他人的任务");
        }

        return todo;
    }

    /**
     * 根据用户名获取用户
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));
    }
}
