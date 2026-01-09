package com.example.cloudnotes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "notes", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_notebook_id", columnList = "notebook_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_updated_at", columnList = "updated_at"),
    @Index(name = "idx_user_notebook", columnList = "user_id, notebook_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT") // ⚠️ 笔记内容通常很长，使用 TEXT 类型存储
    private String content;

    // 🔗 关联笔记本 (多对一)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    @JsonIgnore // 避免 JSON 死循环
    private Notebook notebook;

    // 🔗 关联用户 (多对一)
    // 虽然可以通过 notebook 找到 user，但为了查询方便(比如"查我所有笔记")，直接关联 user 也是常见的做法
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // 🔗 多对多关系 (Note <-> Tag)
    @ManyToMany
    @JoinTable(
            name = "note_tags", // 中间表的名字
            joinColumns = @JoinColumn(name = "note_id"), // 当前表ID
            inverseJoinColumns = @JoinColumn(name = "tag_id") // 对方表ID
    )
    private List<Tag> tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }



}