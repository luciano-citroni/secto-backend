package com.bridge.secto.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@SecurityScheme(
    name = "keycloak",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtido via Keycloak. Use o token de acesso no formato: Bearer {token}"
)
public class OpenApiConfig {

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                    new Server().url("http://localhost:8080").description("Development Server"),
                    new Server().url("").description("Production Server")
                ))
                .info(new Info()
                        .title("Secto Voice Analysis API")
                        .description("API Sectotech para análise de voz. Autenticação via Keycloak (Bearer JWT).")
                        .version(appVersion)
                        .contact(new Contact()
                                .name("Javier Christian")
                                .email("ravichristian14@gmail.com")
                        )
                );
    }
}