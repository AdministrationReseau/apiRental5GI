package com.project.apirental.modules.organization.api;

import com.project.apirental.modules.organization.dto.OrgResponseDTO;
import com.project.apirental.modules.organization.dto.OrgUpdateDTO;
import com.project.apirental.modules.organization.mapper.OrgMapper;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.organization.services.OrganizationService;
import com.project.apirental.modules.subscription.dto.PlanUpgradeRequest;
import com.project.apirental.modules.subscription.dto.SubscriptionRemainingTimeDTO;
import com.project.apirental.modules.subscription.dto.SubscriptionResponseDTO;
import com.project.apirental.modules.subscription.mapper.SubscriptionMapper;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.modules.subscription.services.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
@Tag(name = "Organization Management", description = "Endpoints pour la gestion des organisations")
@SecurityRequirement(name = "bearerAuth") // Indique à Swagger que ces routes nécessitent un token
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;
    private final OrgMapper orgMapper;

     private final SubscriptionService subscriptionService;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Operation(summary = "Obtenir les détails d'une organisation")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')") // Protection par Rôle
    public Mono<ResponseEntity<OrgResponseDTO>> getOrganization(@PathVariable UUID id) {
        return organizationService.getOrganization(id)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour une organisation")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')") // Protection par Rôle
    public Mono<ResponseEntity<OrgResponseDTO>> updateOrganization(
            @PathVariable UUID id,
            @RequestBody OrgUpdateDTO request) {
        return organizationService.updateOrganization(id, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les organisations par ID de plan de souscription")
    @GetMapping("/plan/{planId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<OrgResponseDTO> getOrganizationsByPlan(@PathVariable UUID planId) {
        return organizationRepository.findAllBySubscriptionPlanId(planId)
                .map(orgMapper::toDto);
    }
    @Operation(summary = "Statut de l'abonnement de cette organisation")
    @GetMapping("/{id}/subscription")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> getOrgSubscriptionStatus(@PathVariable UUID id) {
        return organizationRepository.findById(Objects.requireNonNull(id))
                .flatMap(subscriptionService::checkAndDowngrade)
                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .map(plan -> subscriptionMapper.toResponseDTO(org, plan)))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Temps restant avant expiration")
    @GetMapping("/{id}/subscription/remaining")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionRemainingTimeDTO>> getRemainingTime(@PathVariable UUID id) {
        return subscriptionService.getRemainingTime(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Upgrade du plan de l'organisation")
    @PutMapping("/{id}/subscription/upgrade")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> upgradePlan(
            @PathVariable UUID id,
            @RequestBody PlanUpgradeRequest request) {
        return subscriptionService.upgradePlan(id, request.newPlan().name())
                .flatMap(updatedPlan -> organizationRepository.findById(Objects.requireNonNull(id))
                        .map(org -> subscriptionMapper.toResponseDTO(org, updatedPlan)))
                .map(ResponseEntity::ok);
    }

}
