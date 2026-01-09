package com.example.cloudnotes.exception;

/**
 * 待办任务未找到异常
 */
public class TodoNotFoundException extends RuntimeException {
    
    public TodoNotFoundException(String message) {
        super(message);
    }
    
    public TodoNotFoundException(Long todoId) {
        super("任务不存在: ID = " + todoId);
    }
}
