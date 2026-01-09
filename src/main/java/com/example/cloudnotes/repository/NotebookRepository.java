package com.example.cloudnotes.repository;

import com.example.cloudnotes.entity.Notebook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotebookRepository extends JpaRepository<Notebook, Long> {

    // 🔍 自动生成 SQL: SELECT * FROM notebooks WHERE user_id = ?
    // 用来查询指定用户的所有笔记本
    List<Notebook> findByUserId(Long userId);
}