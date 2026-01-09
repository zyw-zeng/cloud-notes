package com.example.cloudnotes.dto;

import lombok.Data;

@Data // 自动生成 Getter/Setter
public class RegisterDTO {
    private String username;
    private String password;
    private String email;
}


@Data
class LoginRequest {
    private String username;
    private String password;
}