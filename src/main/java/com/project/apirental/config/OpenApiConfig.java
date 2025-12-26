package com.project.apirental.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("API Rental").version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    // Groupe 1 : Auth (Public - Login/Register)
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1-authentification")
                .pathsToMatch("/auth/**")
                .build();
    }

    // Groupe 2 : Client (Nécessite token)
    @Bean
    public GroupedOpenApi clientApi() {
        return GroupedOpenApi.builder()
                .group("2-client")
                .pathsToMatch("/api/client/**")
                .build();
    }

    // Groupe 3 : Organisation (Nécessite token)
    @Bean
    public GroupedOpenApi orgApi() {
        return GroupedOpenApi.builder()
                .group("3-organization")
                .pathsToMatch("/api/org/**", "/api/subscriptions/**")
                .build();
    }

}
