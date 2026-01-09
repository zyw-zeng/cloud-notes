package com.example.cloudnotes.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // 标签属于某个用户 (张三的标签，李四不能用)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // 🔗 关键：反向维护多对多关系
    // mappedBy = "tags" 表示关系的维护端在 Note 类里的 tags 字段
    @ManyToMany(mappedBy = "tags")
    @JsonIgnore // 防止 JSON 死循环
    private List<Note> notes;
}