package com.example.cloudnotes.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 告诉 Spring: 我是负责全局异常处理的
public class GlobalExceptionHandler {

    // 捕获所有 RuntimeException (比如我们在 Service 里抛出的 "用户名已存在")
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        // e.getMessage() 就是你在 throw new RuntimeException("这里写的文字")
        return Result.error(e.getMessage());
    }

    // 捕获所有未知异常 (兜底)
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace(); // 在后台打印错误日志
        return Result.error("服务器内部错误，请联系管理员");
    }
}