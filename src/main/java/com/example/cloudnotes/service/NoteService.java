package com.example.cloudnotes.service;

import com.example.cloudnotes.dto.NoteDTO;
import com.example.cloudnotes.dto.NoteSearchDTO;
import com.example.cloudnotes.entity.Note;
import com.example.cloudnotes.entity.Notebook;
import com.example.cloudnotes.entity.Tag;
import com.example.cloudnotes.entity.User;
import com.example.cloudnotes.repository.NoteRepository;
import com.example.cloudnotes.repository.NotebookRepository;
import com.example.cloudnotes.repository.TagRepository;
import com.example.cloudnotes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    // 1. 创建笔记
    @Transactional
    public Note createNote(NoteDTO dto, String username) {
        // 1.1 找人
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        // 1.2 找本子
        Notebook notebook = notebookRepository.findById(dto.getNotebookId())
                .orElseThrow(() -> new RuntimeException("笔记本不存在"));

        // 🛡️ 1.3 安全检查：这个笔记本是你的吗？
        // 如果笔记本的主人ID != 当前用户ID，报错
        if (!notebook.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权操作他人的笔记本！");
        }

        // 处理标签
        List<Tag> tags = null;
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            // 根据前端传的 ID 列表，去数据库把真实的标签对象查出来
            tags = tagRepository.findAllById(dto.getTagIds());
        }

        // 1.4 存笔记
        Note note = Note.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .user(user)
                .notebook(notebook)
                .tags(tags)  //  设置标签
                .build();

        return noteRepository.save(note);
    }

    // 2. 查询某个本子下的笔记
    public List<Note> getNotesInNotebook(Long notebookId, String username) {
        // 同样要检查权限，防止偷看别人的本子
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new RuntimeException("笔记本不存在"));

        // 简单粗暴：比较用户名
        if (!notebook.getUser().getUsername().equals(username)) {
            throw new RuntimeException("无权查看该笔记本");
        }

        return noteRepository.findByNotebookId(notebookId);
    }

    // 3. 更新笔记
    @Transactional
    public Note updateNote(Long noteId, NoteDTO dto, String username) {
        // 查找笔记
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("笔记不存在"));

        // 权限检查：只能修改自己的笔记
        if (!note.getUser().getUsername().equals(username)) {
            throw new RuntimeException("无权修改他人的笔记");
        }

        // 更新基本信息
        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());

        // 如果要换笔记本
        if (dto.getNotebookId() != null && !dto.getNotebookId().equals(note.getNotebook().getId())) {
            Notebook newNotebook = notebookRepository.findById(dto.getNotebookId())
                    .orElseThrow(() -> new RuntimeException("目标笔记本不存在"));
            
            // 检查新笔记本也是自己的
            if (!newNotebook.getUser().getUsername().equals(username)) {
                throw new RuntimeException("无权将笔记移动到他人的笔记本");
            }
            note.setNotebook(newNotebook);
        }

        // 更新标签
        if (dto.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(dto.getTagIds());
            note.setTags(tags);
        }

        return noteRepository.save(note);
    }

    // 4. 删除笔记
    @Transactional
    public void deleteNote(Long noteId, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("笔记不存在"));

        // 权限检查
        if (!note.getUser().getUsername().equals(username)) {
            throw new RuntimeException("无权删除他人的笔记");
        }

        noteRepository.delete(note);
    }

    // 5. 智能搜索笔记
    public List<Note> searchNotes(NoteSearchDTO searchDTO, String username) {
        // 获取用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        // 如果有通用关键词，优先使用关键词搜索
        if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().trim().isEmpty()) {
            // 如果指定了笔记本，在笔记本内搜索
            if (searchDTO.getNotebookId() != null) {
                return noteRepository.searchInNotebook(
                    user.getId(), 
                    searchDTO.getNotebookId(), 
                    searchDTO.getKeyword()
                );
            }
            // 否则全局搜索
            return noteRepository.searchByKeyword(user.getId(), searchDTO.getKeyword());
        }

        // 使用高级组合搜索
        return noteRepository.advancedSearch(
            user.getId(),
            searchDTO.getTitle(),
            searchDTO.getContent(),
            searchDTO.getNotebookId(),
            searchDTO.getTagIds()
        );
    }

    // 6. 根据标签搜索（支持全部匹配或任意匹配）
    public List<Note> searchByTags(List<Long> tagIds, Boolean matchAll, String username) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        List<Note> notes = noteRepository.searchByTags(user.getId(), tagIds);

        // 如果需要匹配所有标签，进行二次过滤
        if (Boolean.TRUE.equals(matchAll)) {
            return notes.stream()
                    .filter(note -> {
                        List<Long> noteTagIds = note.getTags().stream()
                                .map(Tag::getId)
                                .collect(Collectors.toList());
                        return noteTagIds.containsAll(tagIds);
                    })
                    .collect(Collectors.toList());
        }

        return notes;
    }

    // 7. 快速搜索：只搜索标题
    public List<Note> quickSearchByTitle(String title, String username) {
        if (title == null || title.trim().isEmpty()) {
            return new ArrayList<>();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        return noteRepository.searchByTitle(user.getId(), title);
    }

    // 8. 获取用户的所有笔记（用于前端展示全部）
    public List<Note> getAllNotes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        return noteRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}