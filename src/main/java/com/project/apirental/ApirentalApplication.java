package com.project.apirental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableR2dbcAuditing // Active l'audit automatique pour R2DBC (dates de création, etc.)
@EnableAsync         // Indispensable pour que votre @Async dans AuditListener fonctionne
public class ApirentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApirentalApplication.class, args);
    }

}
