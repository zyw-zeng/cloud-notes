package com.example.cloudnotes.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component // 注册为 Spring 组件，方便在其他地方注入使用
public class JwtUtil {

    // 从配置文件中读取 JWT 密钥
    @Value("${jwt.secret}")
    private String secretKey;

    // 从配置文件中读取 Token 过期时间
    @Value("${jwt.expiration}")
    private long expirationTime;

    // 使用 HS256 算法生成签名用的 Key
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * 生成 Token (制牌)
     * @param username 用户名
     * @return 加密后的 Token 字符串
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username) // 工牌上写着谁的名字
                .setIssuedAt(new Date()) // 发证时间
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 盖章 (签名)
                .compact();
    }

    /**
     * 解析 Token (验牌)
     * 从 Token 中提取用户名
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * 验证 Token 是否有效
     * 1. 签名对不对
     * 2. 有没有过期
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token); // 如果解析失败或是过期，这里会抛出异常
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 内部方法：解析核心数据
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}