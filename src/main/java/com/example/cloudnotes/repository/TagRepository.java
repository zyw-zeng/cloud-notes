package com.example.cloudnotes.repository;

import com.example.cloudnotes.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional; // 👈 别忘了导包

public interface TagRepository extends JpaRepository<Tag, Long> {

    // 查我的标签列表
    List<Tag> findByUserId(Long userId);

    // 👇 新增：查某个特定的标签 (根据名字和用户ID)
    // SQL: select * from tags where name = ? and user_id = ?
    Optional<Tag> findByNameAndUserId(String name, Long userId);
}