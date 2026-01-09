package com.example.cloudnotes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置
 * 使用 Redis 分布式缓存（支持集群、持久化）
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存名称常量
     */
    public static final String NOTIFICATION_CACHE = "notifications";
    public static final String NOTIFICATION_COUNT_CACHE = "notificationCount";
    public static final String USER_NOTIFICATIONS_CACHE = "userNotifications";

    /**
     * 配置 Redis 缓存管理器
     * 注入自定义的 ObjectMapper，确保 LocalDateTime 等类型能正确序列化
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // 使用自定义 ObjectMapper 创建序列化器（支持 Java 8 日期时间）
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 设置key序列化方式（String）
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )
                // 设置value序列化方式（JSON）
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                )
                // 默认过期时间 30 分钟
                .entryTtl(Duration.ofMinutes(30))
                // 不缓存 null 值
                .disableCachingNullValues();

        // 为不同缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 通知列表缓存：10分钟
        cacheConfigurations.put(USER_NOTIFICATIONS_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 未读数量缓存：5分钟（更新频繁）
        cacheConfigurations.put(NOTIFICATION_COUNT_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 单个通知缓存：30分钟
        cacheConfigurations.put(NOTIFICATION_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 构建缓存管理器
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()  // 支持事务
                .build();
    }
}
