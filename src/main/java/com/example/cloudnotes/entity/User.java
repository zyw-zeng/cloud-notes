package com.example.cloudnotes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity // 告诉 JPA 这是一个实体类，对应数据库一张表
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data // Lombok: 自动生成 getter/setter/toString
@Builder // Lombok: 提供 Builder 模式构建对象
@NoArgsConstructor // JPA 需要无参构造
@AllArgsConstructor // Builder 需要全参构造
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增主键
    private Long id;

    @Column(unique = true, nullable = false) // 用户名唯一且必填
    private String username;

    @Column(nullable = false)
    private String password; // 密码 (后续存加密后的密文)

    @Column(unique = true)
    private String email;

    private String avatar; // 头像地址

    private LocalDateTime createdAt; // 创建时间

    private LocalDateTime updatedAt; // 更新时间

    // 自动填充创建时间
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // 自动更新修改时间
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}