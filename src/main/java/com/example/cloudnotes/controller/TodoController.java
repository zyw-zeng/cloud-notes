package com.example.cloudnotes.controller;

import com.example.cloudnotes.common.Result;
import com.example.cloudnotes.dto.TodoDTO;
import com.example.cloudnotes.dto.TodoSearchDTO;
import com.example.cloudnotes.entity.Todo;
import com.example.cloudnotes.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "待办任务", description = "待办任务的创建、查询、修改、删除和状态管理")
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @Operation(
        summary = "创建待办任务",
        description = "创建一个新的待办任务，可以设置标题、描述、优先级、截止日期等"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping
    public Result<Todo> createTodo(
            @Parameter(description = "待办任务信息", required = true)
            @Valid @RequestBody TodoDTO dto,
            Principal principal) {
        Todo todo = todoService.createTodo(dto, principal.getName());
        return Result.success(todo);
    }

    @Operation(
        summary = "更新待办任务",
        description = "修改任务的标题、描述、优先级、截止日期等信息"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "403", description = "无权限修改")
    })
    @PutMapping("/{id}")
    public Result<Todo> updateTodo(
            @Parameter(description = "任务ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "更新的任务信息", required = true)
            @Valid @RequestBody TodoDTO dto,
            Principal principal) {
        Todo todo = todoService.updateTodo(id, dto, principal.getName());
        return Result.success(todo);
    }

    @Operation(
        summary = "删除待办任务",
        description = "永久删除指定的待办任务"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "403", description = "无权限删除")
    })
    @DeleteMapping("/{id}")
    public Result<Void> deleteTodo(
            @Parameter(description = "任务ID", required = true, example = "1")
            @PathVariable Long id,
            Principal principal) {
        todoService.deleteTodo(id, principal.getName());
        return Result.success();
    }

    @Operation(
        summary = "获取任务详情",
        description = "根据ID获取单个待办任务的详细信息"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @GetMapping("/{id}")
    public Result<Todo> getTodo(
            @Parameter(description = "任务ID", required = true, example = "1")
            @PathVariable Long id,
            Principal principal) {
        Todo todo = todoService.getTodo(id, principal.getName());
        return Result.success(todo);
    }

    @Operation(
        summary = "标记任务完成",
        description = "将指定的待办任务标记为已完成状态"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @PostMapping("/{id}/complete")
    public Result<Todo> completeTodo(
            @Parameter(description = "任务ID", required = true, example = "1")
            @PathVariable Long id,
            Principal principal) {
        Todo todo = todoService.completeTodo(id, principal.getName());
        return Result.success(todo);
    }

    @Operation(
        summary = "获取所有任务",
        description = "获取当前用户的所有待办任务列表"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/all")
    public Result<List<Todo>> getAllTodos(Principal principal) {
        List<Todo> todos = todoService.getAllTodos(principal.getName());
        return Result.success(todos);
    }

    @Operation(
        summary = "获取今日待办",
        description = "获取截止日期为今天的所有待办任务"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/today")
    public Result<List<Todo>> getTodayTodos(Principal principal) {
        List<Todo> todos = todoService.getTodayTodos(principal.getName());
        return Result.success(todos);
    }

    @Operation(
        summary = "获取本周待办",
        description = "获取截止日期在本周内的所有待办任务"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/week")
    public Result<List<Todo>> getWeekTodos(Principal principal) {
        List<Todo> todos = todoService.getWeekTodos(principal.getName());
        return Result.success(todos);
    }

    @Operation(
        summary = "获取逾期任务",
        description = "获取截止日期已过且未完成的所有任务"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/overdue")
    public Result<List<Todo>> getOverdueTodos(Principal principal) {
        List<Todo> todos = todoService.getOverdueTodos(principal.getName());
        return Result.success(todos);
    }

    @Operation(
        summary = "获取重要任务",
        description = "获取所有标记为重要的待办任务"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/important")
    public Result<List<Todo>> getImportantTodos(Principal principal) {
        List<Todo> todos = todoService.getImportantTodos(principal.getName());
        return Result.success(todos);
    }

    @Operation(
        summary = "获取已完成任务",
        description = "获取所有已标记为完成的任务"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/completed")
    public Result<List<Todo>> getCompletedTodos(Principal principal) {
        List<Todo> todos = todoService.getCompletedTodos(principal.getName());
        return Result.success(todos);
    }

    @Operation(
        summary = "搜索任务",
        description = "根据多种条件组合搜索待办任务，支持关键词、状态、优先级、标签等"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功")
    })
    @PostMapping("/search")
    public Result<List<Todo>> searchTodos(
            @Parameter(description = "搜索条件，支持多种组合", required = true)
            @RequestBody TodoSearchDTO searchDTO,
            Principal principal) {
        List<Todo> todos = todoService.searchTodos(searchDTO, principal.getName());
        return Result.success(todos);
    }
}
