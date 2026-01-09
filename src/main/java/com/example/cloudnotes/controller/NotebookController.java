package com.example.cloudnotes.controller;

import com.example.cloudnotes.common.Result;
import com.example.cloudnotes.dto.NotebookDTO;
import com.example.cloudnotes.entity.Notebook;
import com.example.cloudnotes.service.NotebookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "笔记本管理", description = "笔记本的创建、查询、修改和删除操作")
@RestController
@RequestMapping("/api/notebooks")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    @Operation(
        summary = "创建笔记本",
        description = "创建一个新的笔记本，用于组织和管理笔记"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping
    public Result<Notebook> createNotebook(
            @Parameter(description = "笔记本信息，包含名称和描述", required = true)
            @RequestBody NotebookDTO dto,
            Principal principal) {
        // principal.getName() 就是从 Token 里取出的用户名
        Notebook notebook = notebookService.createNotebook(dto, principal.getName());
        return Result.success(notebook);
    }

    @Operation(
        summary = "获取我的笔记本列表",
        description = "获取当前用户的所有笔记本"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public Result<List<Notebook>> getMyNotebooks(Principal principal) {
        List<Notebook> notebooks = notebookService.getMyNotebooks(principal.getName());
        return Result.success(notebooks);
    }

    @Operation(
        summary = "更新笔记本",
        description = "修改笔记本的名称或描述"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "笔记本不存在"),
        @ApiResponse(responseCode = "403", description = "无权限修改")
    })
    @PutMapping("/{id}")
    public Result<Notebook> updateNotebook(
            @Parameter(description = "笔记本ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "更新的笔记本信息", required = true)
            @RequestBody NotebookDTO dto,
            Principal principal) {
        Notebook notebook = notebookService.updateNotebook(id, dto, principal.getName());
        return Result.success(notebook);
    }

    @Operation(
        summary = "删除笔记本",
        description = "删除指定的笔记本（注意：会同时删除其下的所有笔记）"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "笔记本不存在"),
        @ApiResponse(responseCode = "403", description = "无权限删除")
    })
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotebook(
            @Parameter(description = "笔记本ID", required = true, example = "1")
            @PathVariable Long id,
            Principal principal) {
        notebookService.deleteNotebook(id, principal.getName());
        return Result.success();
    }
}