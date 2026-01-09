package com.example.cloudnotes.config;

import com.example.cloudnotes.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 获取请求头 Authorization
        String authHeader = request.getHeader("Authorization");

        // 2. 检查头是否存在且格式正确 (必须以 Bearer 开头)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 如果没带 Token，直接放行 (交给后面的 Security 拦截器处理，它会发现没登录并拒绝)
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取 Token (去掉 "Bearer " 前缀，剩下就是纯 Token)
        String token = authHeader.substring(7);
        String username = null;

        try {
            // 尝试提取用户名 (如果 Token 篡改或过期，这里会报错)
            username = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            // Token 无效，不做处理，直接进入下一步
        }

        // 4. 验证成功且当前上下文没认证过
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(token)) {
                // 5. 关键步骤：告诉 Spring Security "这个人已登录"
                // 这里我们简单构造一个认证对象 (参数：用户名, 密码null, 权限空列表)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());

                // 把这个认证信息塞进安全上下文，这次请求就算通过了！
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 6. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}