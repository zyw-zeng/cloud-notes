package com.example.cloudnotes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Jackson 配置
 * 解决 Java 8 日期时间类型序列化问题
 * 支持 ISO 8601 格式（如：2026-01-09T08:15 或 2026-01-09T08:15:30）
 */
@Configuration
public class JacksonConfig {

    /**
     * 日期格式
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 日期时间格式化器（支持多种格式）
     * 支持格式：
     * 1. yyyy-MM-dd HH:mm:ss (例如：2026-01-09 08:30:00)
     * 2. yyyy-MM-dd HH:mm (例如：2026-01-09 08:30)
     * 3. yyyy-MM-ddTHH:mm:ss (例如：2026-01-09T08:30:00)
     * 4. yyyy-MM-ddTHH:mm (例如：2026-01-09T08:30)
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .optionalStart()
            .appendLiteral('T')
            .optionalEnd()
            .optionalStart()
            .appendLiteral(' ')
            .optionalEnd()
            .appendPattern("HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .optionalStart()
            .appendPattern(".")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, false)
            .optionalEnd()
            .toFormatter();

    /**
     * 配置 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // 创建 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册 JavaTimeModule（Java 8 日期时间支持）
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 配置 LocalDateTime 序列化和反序列化
        // 序列化使用 ISO_LOCAL_DATE_TIME 格式（输出：2026-01-09T08:15:30）
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        // 反序列化支持多种格式（接受：2026-01-09T08:15 或 2026-01-09T08:15:30）
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));

        // 配置 LocalDate 序列化和反序列化
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));

        // 注册模块
        objectMapper.registerModule(javaTimeModule);

        // 禁用将日期序列化为时间戳（默认会序列化为 [2025, 12, 3, 17, 22, 0] 这种数组格式）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
