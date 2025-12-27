package com.project.apirental.modules.subscription.api;

import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.subscription.domain.SubscriptionCatalog;
import com.project.apirental.modules.subscription.dto.PlanUpgradeRequest;
import com.project.apirental.modules.subscription.dto.SubscriptionResponseDTO;
import com.project.apirental.modules.subscription.mapper.SubscriptionMapper;
import com.project.apirental.modules.subscription.services.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Management")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionMapper subscriptionMapper;
    private final OrganizationRepository organizationRepository;

    @Operation(summary = "Lister le catalogue des plans")
    @GetMapping("/plans")
    public Flux<SubscriptionCatalog.SubscriptionPlan> getCatalog() {
        return Flux.fromIterable(SubscriptionCatalog.PLANS.values());
    }

    @Operation(summary = "Obtenir le statut actuel")
    @GetMapping("/status/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> getOrganizationPlan(@PathVariable UUID orgId) {
        return organizationRepository.findById(orgId)
                .flatMap(subscriptionService::checkAndDowngrade)
                .map(subscriptionMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation non trouvée")));
    }

    @Operation(summary = "Mettre à jour le plan (Upgrade)")
    @PutMapping("/upgrade/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> upgradePlan(
            @PathVariable UUID orgId,
            @RequestBody PlanUpgradeRequest request) {
        
        // request.newPlan() est un PlanType (Enum), donc .name() renvoie "PRO", "FREE", etc.
        String planName = request.newPlan().name();

        return subscriptionService.upgradePlan(orgId, planName)
                .flatMap(plan -> organizationRepository.findById(orgId))
                .map(subscriptionMapper::toResponseDTO)
                .map(ResponseEntity::ok);
    }

   @Operation(summary = "Récupérer tous les véhicules liés à un type de plan")
    @GetMapping("/vehicles-by-plan/{planName}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<Object> getVehiclesByPlan(@PathVariable String planName) {
        // planName est une String, le repository accepte maintenant une String
        return organizationRepository.findAllBySubscriptionPlanName(planName.toUpperCase())
                .flatMap(org -> {
                    // Logique future pour les véhicules
                    return Flux.empty(); 
                });
    }
}