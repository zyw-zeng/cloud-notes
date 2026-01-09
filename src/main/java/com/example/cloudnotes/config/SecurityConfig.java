package com.example.cloudnotes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // 告诉 Spring 这是一个配置类
@EnableWebSecurity // 开启 Web 安全功能
@RequiredArgsConstructor
public class SecurityConfig {
    // 声明过滤器
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    // 1. 定义一个“密码加密器” Bean
    // BCrypt 是目前最安全的加密算法之一
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. 定义“安全过滤器链” (SecurityFilterChain)
    // 这里规定了哪些接口需要登录，哪些不需要
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF 保护 (因为我们是前后端分离，用 Token 认证，不需要 CSRF，关了省事)
                .csrf(csrf -> csrf.disable())

                // 配置请求拦截规则
                .authorizeHttpRequests(auth -> auth
                        // 允许所有人访问 /api/auth 下的所有接口 (注册、登录)
                        .requestMatchers("/api/auth/**").permitAll()
                        // 允许 WebSocket 连接（SockJS 需要）
                        .requestMatchers("/ws/**").permitAll()
                        // 允许访问 Swagger UI 和 API 文档
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // 其他任何接口，都必须登录后才能访问
                        .anyRequest().authenticated()
                ).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}