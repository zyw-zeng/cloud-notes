package com.example.cloudnotes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "笔记搜索条件对象，支持多种搜索条件组合")
public class NoteSearchDTO {
    
    @Schema(description = "搜索关键词，将同时在标题和内容中进行模糊匹配", example = "会议")
    private String keyword;
    
    @Schema(description = "仅搜索标题的关键词", example = "项目计划")
    private String title;
    
    @Schema(description = "仅搜索内容的关键词", example = "需求分析")
    private String content;
    
    @Schema(description = "标签ID列表，支持多标签组合搜索", example = "[1, 2, 3]")
    private List<Long> tagIds;
    
    @Schema(description = "笔记本ID，限定在指定笔记本内搜索", example = "1")
    private Long notebookId;
    
    @Schema(description = "标签匹配模式：true=必须包含所有标签(AND)，false=包含任一标签即可(OR)", example = "false", defaultValue = "false")
    private Boolean matchAllTags = false;
}
