package com.example.cloudnotes.service;

import com.example.cloudnotes.dto.RegisterDTO;
import com.example.cloudnotes.entity.User;
import com.example.cloudnotes.repository.UserRepository;
import lombok.RequiredArgsConstructor; // 这一行可能需要你的 IDEA 自动导入
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.cloudnotes.utils.JwtUtil; //  导入工具类

@Service // 1. 告诉 Spring 这是一个业务逻辑类，把它存进容器里
@RequiredArgsConstructor // 2. Lombok 自动生成构造函数，帮我们注入 repository
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // 入密码加密器
    private final JwtUtil jwtUtil; //  注入制牌机

    // 处理注册逻辑
    public User register(RegisterDTO request) {
        // 1. 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在！");
        }

        // 2. 检查邮箱
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已存在！");
        }

        // 3. 构建 User 实体 (把 DTO 转换成 Entity)
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))   // 使用 passwordEncoder.encode() 进行加密
                .email(request.getEmail())
                .avatar("https://i.pravatar.cc/150?img=1") // 给个默认头像
                .build();

        // 4. 保存到数据库
        return userRepository.save(user);
    }


    public String login(String username, String password) {
        // 第一步：找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 第二步：对暗号 (比对密码)
        // 注意：不能直接比 equals，因为数据库里是加密的，必须用 matches 方法
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 第三步：验证通过，发工牌 (生成 Token)
        return jwtUtil.generateToken(user.getUsername());
    }
}