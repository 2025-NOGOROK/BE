package com.example.Easeplan.global.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;      // <-- 서버 URL 명시를 위한 import
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;                               // <-- 여러 서버 URL을 위한 import

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("accessToken",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                ))
                .servers(List.of(
                        new Server().url("https://recommend.ai.kr").description("Production"),
                        new Server().url("http://localhost:8080").description("Local")
                ))
                .info(new Info()
                        .title("NOGOROK API")
                        .description("API 설명")
                        .version("v1.0"));
    }
}
