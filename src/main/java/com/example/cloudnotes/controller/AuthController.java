package com.example.cloudnotes.controller;

import com.example.cloudnotes.common.Result;
import com.example.cloudnotes.dto.RegisterDTO;
import com.example.cloudnotes.entity.User;
import com.example.cloudnotes.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "用户认证", description = "用户注册、登录等认证相关操作")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(
        summary = "用户注册",
        description = "创建新用户账号，需要提供用户名、密码和邮箱"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功，返回用户ID"),
        @ApiResponse(responseCode = "400", description = "请求参数错误或用户名已存在")
    })
    @PostMapping("/register")
    public Result<?> register(
            @Parameter(description = "注册信息，包含用户名、密码和邮箱", required = true)
            @RequestBody RegisterDTO registerDTO) {
        // @RequestBody 的作用：把前端传来的 JSON 数据 ({"username":"..."}) 自动转成 RegisterDTO 对象
        User registeredUser = userService.register(registerDTO);
        // 注册成功，返回用户ID
        return Result.success(Map.of("userId", registeredUser.getId()));
    }

    @Operation(
        summary = "用户登录",
        description = "使用用户名和密码登录，成功后返回JWT Token用于后续接口认证"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功，返回JWT Token"),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public Result<?> login(
            @Parameter(description = "登录凭证，包含用户名和密码", required = true)
            @RequestBody RegisterDTO loginRequest) {
        // 调用 Service 登录，获取 Token
        String token = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        // 返回 Token 给前端
        return Result.success(Map.of("token", token));
    }
}