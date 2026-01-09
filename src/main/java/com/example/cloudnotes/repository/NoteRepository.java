package com.example.cloudnotes.repository;

import com.example.cloudnotes.entity.Note;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    // 1. 查询某个笔记本下的所有笔记
    // 使用 EntityGraph 防止 N+1 查询问题
    @EntityGraph(attributePaths = {"tags", "notebook", "user"})
    List<Note> findByNotebookId(Long notebookId);

    // 2. 查询某个用户的所有笔记 (按时间倒序)
    // 使用 EntityGraph 一次性加载关联数据
    @EntityGraph(attributePaths = {"tags", "notebook"})
    List<Note> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 3. 根据标题和内容关键词搜索（用户自己的笔记）
    @Query("SELECT DISTINCT n FROM Note n WHERE n.user.id = :userId " +
           "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Note> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    // 4. 根据标题搜索
    @Query("SELECT n FROM Note n WHERE n.user.id = :userId " +
           "AND LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Note> searchByTitle(@Param("userId") Long userId, @Param("title") String title);
    
    // 5. 根据内容搜索
    @Query("SELECT n FROM Note n WHERE n.user.id = :userId " +
           "AND LOWER(n.content) LIKE LOWER(CONCAT('%', :content, '%'))")
    List<Note> searchByContent(@Param("userId") Long userId, @Param("content") String content);
    
    // 6. 根据标签搜索（匹配任意标签）
    @Query("SELECT DISTINCT n FROM Note n JOIN n.tags t " +
           "WHERE n.user.id = :userId AND t.id IN :tagIds")
    List<Note> searchByTags(@Param("userId") Long userId, @Param("tagIds") List<Long> tagIds);
    
    // 7. 在指定笔记本内搜索关键词
    @Query("SELECT DISTINCT n FROM Note n WHERE n.user.id = :userId " +
           "AND n.notebook.id = :notebookId " +
           "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Note> searchInNotebook(@Param("userId") Long userId, 
                                 @Param("notebookId") Long notebookId, 
                                 @Param("keyword") String keyword);
    
    // 8. 组合搜索：标题、内容、标签
    @Query("SELECT DISTINCT n FROM Note n LEFT JOIN n.tags t " +
           "WHERE n.user.id = :userId " +
           "AND (:title IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:content IS NULL OR LOWER(n.content) LIKE LOWER(CONCAT('%', :content, '%'))) " +
           "AND (:notebookId IS NULL OR n.notebook.id = :notebookId) " +
           "AND (:tagIds IS NULL OR t.id IN :tagIds)")
    List<Note> advancedSearch(@Param("userId") Long userId,
                             @Param("title") String title,
                             @Param("content") String content,
                             @Param("notebookId") Long notebookId,
                             @Param("tagIds") List<Long> tagIds);
}