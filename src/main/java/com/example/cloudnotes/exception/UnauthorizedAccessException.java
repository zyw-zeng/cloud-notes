package com.example.cloudnotes.exception;

/**
 * 未授权访问异常
 */
public class UnauthorizedAccessException extends RuntimeException {
    
    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedAccessException() {
        super("无权操作他人的资源");
    }
}
