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
                .info(new Info()
                        .title("API Rental Platform")
                        .version("1.0")
                        .description("Documentation de l'API de gestion de location de véhicules."))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    // ========================================================================
    // GROUPE 1 : AUTHENTIFICATION & PUBLIC
    // ========================================================================
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1-Public-Auth")
                .pathsToMatch(
                        "/auth/**",                  // Login, Register, Refresh Token
                        "/api/media/**",             // Upload de fichiers
                        "/api/subscriptions/plans"   // Catalogue des plans (Public)
                )
                .build();
    }

    // ========================================================================
    // GROUPE 2 : ESPACE CLIENT (Application Mobile / Site Web)
    // ========================================================================
    @Bean
    public GroupedOpenApi clientApi() {
        return GroupedOpenApi.builder()
                .group("2-Espace-Client")
                .pathsToMatch(
                        // --- Recherche & Consultation ---
                        "/api/vehicles/available",
                        "/api/vehicles/search",
                        "/api/vehicles/{id}/details",       // Détails complets véhicule
                        "/api/vehicles/categories/all",
                        "/api/drivers/{id}/details",        // Détails chauffeur
                        "/api/agencies/all",
                        "/api/agencies/{id}",
                        "/api/reviews/**",                  // Lire et poster des avis

                        // --- Processus de Location ---
                        "/api/rentals/init",                // Créer un devis/réservation
                        "/api/rentals/{id}/pay",            // Payer
                        "/api/rentals/{id}/cancel",         // Annuler
                        "/api/rentals/{id}/end-signal",     // Signaler fin de course
                        "/api/rentals/client/**",           // Listings spécifiques client
                        "/api/rentals/my-rentals",          // Mes locations

                        // --- Données Personnelles ---
                        "/api/notifications/client/**",     // Mes notifications
                        "/api/notifications/{id}/read",     // Marquer comme lu
                        "/api/transactions/client/**"       // Mon historique financier
                )
                .build();
    }

    // ========================================================================
    // GROUPE 3 : ESPACE ORGANISATION & AGENCE (Back-Office)
    // ========================================================================
    @Bean
    public GroupedOpenApi orgApi() {
        return GroupedOpenApi.builder()
                .group("3-Espace-Gestion")
                .pathsToMatch(
                        // --- Administration ---
                        "/api/org/**",                      // Gestion de l'organisation
                        "/api/agencies/**",                 // CRUD Agences
                        "/api/subscriptions/**",            // Gestion abonnement (Upgrade, Renew)

                        // --- Ressources Humaines ---
                        "/api/staff/**",                    // Gestion du personnel
                        "/api/postes/**",                   // Gestion des postes
                        "/api/permissions/**",              // Liste des permissions

                        // --- Gestion de Flotte ---
                        "/api/vehicles/**",                 // CRUD Véhicules (Org/Agency endpoints)
                        "/api/drivers/**",                  // CRUD Chauffeurs
                        "/api/vehicles/categories/**",      // Gestion catégories

                        // --- Gestion des Locations ---
                        "/api/rentals/agency/**",           // Liste locations par agence
                        "/api/rentals/org/**",              // Liste locations par organisation
                        "/api/rentals/{id}/start",          // Valider départ
                        "/api/rentals/{id}/validate-return",// Valider retour

                        // --- Suivi & Finances ---
                        "/api/stats/**",                    // Dashboards & Rapports
                        "/api/transactions/org/**",         // Grand livre Organisation
                        "/api/transactions/agency/**",      // Transactions Agence

                        // --- Communication ---
                        "/api/notifications/org/**",        // Notifs Organisation
                        "/api/notifications/agency/**",     // Notifs Agence
                        "/api/notifications/{id}/read"      // Marquer comme lu
                )
                .build();
    }
}
