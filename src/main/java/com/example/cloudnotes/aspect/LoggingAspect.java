package com.example.cloudnotes.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 日志切面 - 记录所有 Controller 层的请求和响应
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {

    private final ObjectMapper objectMapper;

    /**
     * 定义切点：拦截 controller 包下的所有方法
     */
    @Pointcut("execution(* com.example.cloudnotes.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录请求详情和响应
     */
    @Around("controllerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // 记录请求信息
            log.info("========== 请求开始 ==========");
            log.info("请求URL: {} {}", request.getMethod(), request.getRequestURL());
            log.info("请求IP: {}", getClientIp(request));
            log.info("请求方法: {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
            
            // 记录请求参数（敏感信息需要脱敏）
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                try {
                    // 过滤掉 HttpServletRequest、HttpServletResponse 等非业务参数
                    Object[] businessArgs = Arrays.stream(args)
                            .filter(arg -> arg != null && !isFrameworkClass(arg))
                            .toArray();
                    if (businessArgs.length > 0) {
                        log.info("请求参数: {}", objectMapper.writeValueAsString(businessArgs));
                    }
                } catch (Exception e) {
                    log.warn("请求参数序列化失败: {}", e.getMessage());
                }
            }
        }

        Object result = null;
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录响应信息
            log.info("响应结果: {}", objectMapper.writeValueAsString(result));
            log.info("执行耗时: {} ms", executionTime);
            log.info("========== 请求结束 ==========");
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("请求异常 - 耗时: {} ms", executionTime);
            log.error("异常信息: ", e);
            log.info("========== 请求异常结束 ==========");
            throw e;
        }
    }

    /**
     * 后置通知：方法正常执行后
     */
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        // 可以在这里记录特定的业务日志
    }

    /**
     * 异常通知：方法抛出异常后
     */
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error("方法执行异常: {}.{}", 
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
        log.error("异常类型: {}", e.getClass().getName());
        log.error("异常消息: {}", e.getMessage());
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 判断是否是框架类（不需要记录）
     */
    private boolean isFrameworkClass(Object arg) {
        String className = arg.getClass().getName();
        return className.startsWith("javax.servlet") 
            || className.startsWith("jakarta.servlet")
            || className.startsWith("org.springframework.web")
            || className.startsWith("java.security.Principal");
    }
}
