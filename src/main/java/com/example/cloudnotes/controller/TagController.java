package com.example.cloudnotes.controller;

import com.example.cloudnotes.common.Result;
import com.example.cloudnotes.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理", description = "标签的创建、查询、修改和删除操作")
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(
        summary = "创建标签",
        description = "创建一个新的标签，用于给笔记和待办任务分类"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误或标签名已存在")
    })
    @PostMapping
    public Result<com.example.cloudnotes.entity.Tag> createTag(
            @Parameter(description = "标签信息，包含标签名称", required = true)
            @RequestBody TagDTO dto,
            Principal principal) {
        com.example.cloudnotes.entity.Tag tag = tagService.createTag(dto.getName(), principal.getName());
        return Result.success(tag);
    }

    @Operation(
        summary = "获取我的标签列表",
        description = "获取当前用户的所有标签"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public Result<List<com.example.cloudnotes.entity.Tag>> getMyTags(Principal principal) {
        List<com.example.cloudnotes.entity.Tag> tags = tagService.getMyTags(principal.getName());
        return Result.success(tags);
    }

    @Operation(
        summary = "更新标签",
        description = "修改标签的名称"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "标签不存在"),
        @ApiResponse(responseCode = "403", description = "无权限修改")
    })
    @PutMapping("/{id}")
    public Result<com.example.cloudnotes.entity.Tag> updateTag(
            @Parameter(description = "标签ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "更新的标签信息", required = true)
            @RequestBody TagDTO dto,
            Principal principal) {
        com.example.cloudnotes.entity.Tag tag = tagService.updateTag(id, dto.getName(), principal.getName());
        return Result.success(tag);
    }

    @Operation(
        summary = "删除标签",
        description = "删除指定的标签（不会删除已关联的笔记和任务）"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "标签不存在"),
        @ApiResponse(responseCode = "403", description = "无权限删除")
    })
    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(
            @Parameter(description = "标签ID", required = true, example = "1")
            @PathVariable Long id,
            Principal principal) {
        tagService.deleteTag(id, principal.getName());
        return Result.success();
    }
}

@Data
@Schema(description = "标签数据传输对象")
class TagDTO {
    @Schema(description = "标签名称", example = "工作", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
    private String name;
}