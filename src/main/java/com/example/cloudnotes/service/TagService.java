package com.example.cloudnotes.service;

import com.example.cloudnotes.entity.Tag;
import com.example.cloudnotes.entity.User;
import com.example.cloudnotes.repository.TagRepository;
import com.example.cloudnotes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    // 创建标签
    public Tag createTag(String name, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. 先去数据库查一查，这个用户有没有这个标签？
        Optional<Tag> existingTag = tagRepository.findByNameAndUserId(name, user.getId());

        if (existingTag.isPresent()) {
            return existingTag.get();
        }

        Tag tag = Tag.builder()
                .name(name)
                .user(user)
                .build();
        return tagRepository.save(tag);
    }

    // 获取我的标签列表
    public List<Tag> getMyTags(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return tagRepository.findByUserId(user.getId());
    }

    // 更新标签
    public Tag updateTag(Long tagId, String name, String username) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        // 权限检查
        if (!tag.getUser().getUsername().equals(username)) {
            throw new RuntimeException("无权修改他人的标签");
        }

        // 检查新名称是否与该用户的其他标签重复
        Optional<Tag> existingTag = tagRepository.findByNameAndUserId(name, tag.getUser().getId());
        if (existingTag.isPresent() && !existingTag.get().getId().equals(tagId)) {
            throw new RuntimeException("标签名称已存在");
        }

        tag.setName(name);
        return tagRepository.save(tag);
    }

    // 删除标签
    public void deleteTag(Long tagId, String username) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        // 权限检查
        if (!tag.getUser().getUsername().equals(username)) {
            throw new RuntimeException("无权删除他人的标签");
        }

        tagRepository.delete(tag);
    }
}