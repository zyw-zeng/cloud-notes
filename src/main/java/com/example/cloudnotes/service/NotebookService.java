package com.example.cloudnotes.service;

import com.example.cloudnotes.dto.NotebookDTO;
import com.example.cloudnotes.entity.Notebook;

import com.example.cloudnotes.entity.User;
import com.example.cloudnotes.repository.NotebookRepository;

import com.example.cloudnotes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotebookService {

    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;


    // 1. 创建笔记本
    @Transactional // 开启事务：要么全部成功，要么全部回滚
    public Notebook createNotebook(NotebookDTO dto, String username) {
        // 先根据 Token 里的用户名找到数据库里的 User 对象
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));


        Notebook notebook = Notebook.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .user(user) // 👈 关键：这里把笔记本属于谁设置进去！
                .build();

        return notebookRepository.save(notebook);
    }

    // 2. 获取当前用户的所有笔记本
    public List<Notebook> getMyNotebooks(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未找到"));

        // 调用 Repository 刚才写的 findByUserId
        return notebookRepository.findByUserId(user.getId());
    }

    // 3. 更新笔记本
    @Transactional
    public Notebook updateNotebook(Long notebookId, NotebookDTO dto, String username) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new RuntimeException("笔记本不存在"));

        // 
        if (!notebook.getUser().getUsername().equals(username)) {
            throw new RuntimeException("无权修改他人的笔记本");
        }

        // 
        notebook.setName(dto.getName());
        notebook.setDescription(dto.getDescription());

        return notebookRepository.save(notebook);
    }

    // 4. 删除笔记本
    @Transactional
    public void deleteNotebook(Long notebookId, String username) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new RuntimeException("笔记本不存在"));

        // 
        if (!notebook.getUser().getUsername().equals(username)) {
            throw new RuntimeException("无权删除他人的笔记本");
        }

        // 
        // 
        notebookRepository.delete(notebook);
    }

}