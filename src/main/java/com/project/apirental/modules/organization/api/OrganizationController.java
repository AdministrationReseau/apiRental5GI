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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
@Tag(name = "Organization Management", description = "Endpoints pour la gestion des organisations")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;
    private final OrgMapper orgMapper;

    private final SubscriptionService subscriptionService;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Operation(summary = "Lister toutes les organisations (Admin)")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<OrgResponseDTO> getAll() {
        return organizationService.getAllOrganizations();
    }

    @Operation(summary = "Obtenir les détails d'une organisation")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('ADMIN')")
    public Mono<ResponseEntity<OrgResponseDTO>> getOrganization(@PathVariable UUID id) {
        return organizationService.getOrganization(id)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour une organisation (JSON)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<OrgResponseDTO>> updateOrganization(
            @PathVariable UUID id,
            @RequestBody OrgUpdateDTO request) {
        return organizationService.updateOrganization(id, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour une organisation avec Fichiers (Multipart)")
    @PutMapping(value = "/{id}/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<OrgResponseDTO>> updateOrganizationMultipart(
            @PathVariable UUID id,
            // Champs texte facultatifs
            @RequestPart(value = "name", required = false) String name,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "phone", required = false) String phone,
            @RequestPart(value = "email", required = false) String email,
            @RequestPart(value = "address", required = false) String address,
            @RequestPart(value = "city", required = false) String city,
            @RequestPart(value = "postalCode", required = false) String postalCode,
            @RequestPart(value = "region", required = false) String region,
            @RequestPart(value = "website", required = false) String website,
            @RequestPart(value = "registrationNumber", required = false) String registrationNumber,
            @RequestPart(value = "taxNumber", required = false) String taxNumber,
            // Fichiers facultatifs
            @RequestPart(value = "logo", required = false) FilePart logoFile,
            @RequestPart(value = "license", required = false) FilePart licenseFile
    ) {
        // Reconstruction manuelle du DTO à partir des parts
        // Note: Les champs non présents seront null, ce qui est géré par le service
        OrgUpdateDTO partialDto = new OrgUpdateDTO(
                name, description, address, city, postalCode, region, phone, email, website,
                null, // timezone géré à part ou via autre endpoint si complexe
                null, // logoUrl géré par le fichier
                registrationNumber, taxNumber
        );

        return organizationService.updateOrganizationWithMedia(id, partialDto, logoFile, licenseFile)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les organisations par ID de plan de souscription")
    @GetMapping("/plan/{planId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('ADMIN')")
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
