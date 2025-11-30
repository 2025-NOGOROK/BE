package com.example.Easeplan.global.auth.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                //  모든 API에 토큰 자동 부착
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                //  Same Origin + 운영 + (선택) 로컬 직접 호출 옵션
                .servers(List.of(
                        new Server().url("/").description("Same Origin"),           // UI 오리진 그대로
                        new Server().url("https://recommend.ai.kr").description("Production"),
                        new Server().url("http://localhost:8080").description("Local (직접 호출)")
                ))
                .info(new Info().title("NOGOROK API").version("v1.0").description("API 설명"));
    }
}
