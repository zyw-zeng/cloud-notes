package com.example.cloudnotes.repository;

import com.example.cloudnotes.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// 泛型 <User, Long> 的意思是：这个仓库管理 "User" 表，主键类型是 "Long"
public interface UserRepository extends JpaRepository<User, Long> {

    // 🪄 魔法方法：
    // 我们只需要定义这个方法名，JPA 会自动将其翻译成 SQL:
    // SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // 同理，检查邮箱是否存在
    boolean existsByEmail(String email);

    // 检查用户名是否存在
    boolean existsByUsername(String username);
}