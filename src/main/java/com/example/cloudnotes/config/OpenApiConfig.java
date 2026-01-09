package com.example.cloudnotes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudNotesOpenAPI() {
        return new OpenAPI()
                .openapi("3.0.1")  // 指定 OpenAPI 规范版本
                .info(new Info()
                        .title("Cloud Notes API")
                        .description("云笔记系统 RESTful API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Cloud Notes Team")
                                .email("support@cloudnotes.com")
                                .url("https://github.com/your-repo/cloud-notes"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("开发环境"),
                        new Server()
                                .url("https://api.cloudnotes.com")
                                .description("生产环境")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 认证令牌")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
