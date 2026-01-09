package com.example.cloudnotes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "笔记数据传输对象")
public class NoteDTO {
    
    @Schema(description = "笔记标题", example = "项目会议纪要", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    private String title;
    
    @Schema(description = "笔记内容，支持Markdown格式", example = "# 会议内容\n\n1. 讨论项目进度\n2. 确定下一步计划")
    private String content;
    
    @Schema(description = "笔记本ID，指定笔记所属的笔记本", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long notebookId;
    
    @Schema(description = "标签ID列表，为笔记添加分类标签", example = "[1, 2, 3]")
    private List<Long> tagIds;
}