package com.example.cloudnotes.specification;

import com.example.cloudnotes.dto.TodoSearchDTO;
import com.example.cloudnotes.entity.Todo;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 待办任务搜索条件构建器
 */
public class TodoSpecification {

    /**
     * 根据搜索条件构建 Specification
     */
    public static Specification<Todo> buildSearchSpec(Long userId, TodoSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 用户ID过滤（必需）
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            
            // 关键词搜索（标题或描述）
            if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().trim().isEmpty()) {
                String keyword = "%" + searchDTO.getKeyword().toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), keyword);
                Predicate descMatch = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), keyword);
                predicates.add(criteriaBuilder.or(titleMatch, descMatch));
            }
            
            // 状态筛选
            if (searchDTO.getStatuses() != null && !searchDTO.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(searchDTO.getStatuses()));
            }
            
            // 优先级筛选
            if (searchDTO.getPriorities() != null && !searchDTO.getPriorities().isEmpty()) {
                predicates.add(root.get("priority").in(searchDTO.getPriorities()));
            }
            
            // 只显示重要任务
            if (Boolean.TRUE.equals(searchDTO.getOnlyImportant())) {
                predicates.add(criteriaBuilder.equal(root.get("isImportant"), true));
            }
            
            // 截止日期范围
            if (searchDTO.getDueDateStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("dueDate"), searchDTO.getDueDateStart()));
            }
            if (searchDTO.getDueDateEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("dueDate"), searchDTO.getDueDateEnd()));
            }
            
            // 只显示逾期任务
            if (Boolean.TRUE.equals(searchDTO.getOnlyOverdue())) {
                predicates.add(criteriaBuilder.lessThan(
                    root.get("dueDate"), LocalDateTime.now()));
                predicates.add(root.get("status").in(
                    Todo.TodoStatus.TODO, Todo.TodoStatus.IN_PROGRESS));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
