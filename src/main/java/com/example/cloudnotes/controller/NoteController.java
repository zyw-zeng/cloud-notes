package com.example.cloudnotes.controller;

import com.example.cloudnotes.common.Result;
import com.example.cloudnotes.dto.NoteDTO;
import com.example.cloudnotes.dto.NoteSearchDTO;
import com.example.cloudnotes.entity.Note;
import com.example.cloudnotes.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "笔记管理", description = "笔记的增删改查、搜索等操作")
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @Operation(
        summary = "创建笔记",
        description = "在指定笔记本中创建一篇新笔记，可以添加标题、内容和标签"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(schema = @Schema(implementation = Note.class))),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录")
    })
    @PostMapping
    public Result<Note> createNote(
            @Parameter(description = "笔记信息，包含标题、内容、笔记本ID和标签ID列表", required = true)
            @RequestBody NoteDTO dto,
            Principal principal) {
        Note note = noteService.createNote(dto, principal.getName());
        return Result.success(note);
    }

    @Operation(
        summary = "获取笔记本下的所有笔记",
        description = "根据笔记本ID获取该笔记本下的所有笔记列表"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "笔记本不存在")
    })
    @GetMapping
    public Result<List<Note>> getNotes(
            @Parameter(description = "笔记本ID", required = true, example = "1")
            @RequestParam Long notebookId,
            Principal principal) {
        List<Note> notes = noteService.getNotesInNotebook(notebookId, principal.getName());
        return Result.success(notes);
    }

    @Operation(
        summary = "更新笔记",
        description = "更新指定笔记的标题、内容或标签信息"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "笔记不存在"),
        @ApiResponse(responseCode = "403", description = "无权限修改此笔记")
    })
    @PutMapping("/{id}")
    public Result<Note> updateNote(
            @Parameter(description = "笔记ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "更新的笔记信息", required = true)
            @RequestBody NoteDTO dto,
            Principal principal) {
        Note note = noteService.updateNote(id, dto, principal.getName());
        return Result.success(note);
    }

    @Operation(
        summary = "删除笔记",
        description = "永久删除指定的笔记（不可恢复）"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "笔记不存在"),
        @ApiResponse(responseCode = "403", description = "无权限删除此笔记")
    })
    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(
            @Parameter(description = "笔记ID", required = true, example = "1")
            @PathVariable Long id,
            Principal principal) {
        noteService.deleteNote(id, principal.getName());
        return Result.success();
    }

    @Operation(
        summary = "智能搜索笔记",
        description = "支持多条件组合搜索：关键词、标题、内容、标签等，可灵活组合使用"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功")
    })
    @PostMapping("/search")
    public Result<List<Note>> searchNotes(
            @Parameter(description = "搜索条件，支持关键词、标题、内容、标签ID等多种组合", required = true)
            @RequestBody NoteSearchDTO searchDTO,
            Principal principal) {
        List<Note> notes = noteService.searchNotes(searchDTO, principal.getName());
        return Result.success(notes);
    }

    @Operation(
        summary = "快速搜索",
        description = "根据关键词快速搜索笔记的标题和内容（模糊匹配）"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功")
    })
    @GetMapping("/search/quick")
    public Result<List<Note>> quickSearch(
            @Parameter(description = "搜索关键词，将在标题和内容中进行模糊匹配", required = true, example = "会议")
            @RequestParam String keyword,
            Principal principal) {
        NoteSearchDTO searchDTO = new NoteSearchDTO();
        searchDTO.setKeyword(keyword);
        List<Note> notes = noteService.searchNotes(searchDTO, principal.getName());
        return Result.success(notes);
    }

    @Operation(
        summary = "按标签搜索",
        description = "根据一个或多个标签搜索笔记，支持'包含任一标签'或'包含所有标签'两种模式"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功")
    })
    @GetMapping("/search/tags")
    public Result<List<Note>> searchByTags(
            @Parameter(description = "标签ID列表，多个ID用逗号分隔", required = true, example = "1,2,3")
            @RequestParam List<Long> tagIds,
            @Parameter(description = "匹配模式：true=必须包含所有标签，false=包含任一标签即可", example = "false")
            @RequestParam(defaultValue = "false") Boolean matchAll,
            Principal principal) {
        List<Note> notes = noteService.searchByTags(tagIds, matchAll, principal.getName());
        return Result.success(notes);
    }

    @Operation(
        summary = "按标题搜索",
        description = "仅在笔记标题中进行模糊搜索"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功")
    })
    @GetMapping("/search/title")
    public Result<List<Note>> searchByTitle(
            @Parameter(description = "标题关键词", required = true, example = "项目计划")
            @RequestParam String title,
            Principal principal) {
        List<Note> notes = noteService.quickSearchByTitle(title, principal.getName());
        return Result.success(notes);
    }

    @Operation(
        summary = "获取所有笔记",
        description = "获取当前用户的所有笔记列表，按创建时间倒序排列"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/all")
    public Result<List<Note>> getAllNotes(Principal principal) {
        List<Note> notes = noteService.getAllNotes(principal.getName());
        return Result.success(notes);
    }
}