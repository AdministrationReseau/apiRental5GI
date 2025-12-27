package com.project.apirental.modules.organization.services;

import com.project.apirental.modules.organization.dto.OrgResponseDTO;
import com.project.apirental.modules.organization.dto.OrgUpdateDTO;
import com.project.apirental.modules.organization.mapper.OrgMapper;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository planRepository;
    private final OrgMapper orgMapper;
    private final ApplicationEventPublisher eventPublisher;

    public Mono<OrgResponseDTO> getOrganization(UUID id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("id must not be null"));
        }
        return organizationRepository.findById(id)
                .map(orgMapper::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Organization not found")));
    }

    @Transactional
    public Mono<OrgResponseDTO> updateOrganization(UUID id, OrgUpdateDTO request) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("id must not be null"));
        }
        return organizationRepository.findById(id)
            .flatMap(org -> {
                // Mapping manuel (ou via MapStruct si configuré plus tard)
                if(request.name() != null) org.setName(request.name());
                if(request.description() != null) org.setDescription(request.description());
                if(request.address() != null) org.setAddress(request.address());
                if(request.city() != null) org.setCity(request.city());
                if(request.postalCode() != null) org.setPostalCode(request.postalCode());
                if(request.region() != null) org.setRegion(request.region());
                if(request.phone() != null) org.setPhone(request.phone());
                if(request.email() != null) org.setEmail(request.email());
                if(request.website() != null) org.setWebsite(request.website());
                if(request.timezone() != null) org.setTimezone(request.timezone());
                if(request.logoUrl() != null) org.setLogoUrl(request.logoUrl());
                if(request.registrationNumber() != null) org.setRegistrationNumber(request.registrationNumber());
                if(request.taxNumber() != null) org.setTaxNumber(request.taxNumber());

                if (org != null) {
                    return organizationRepository.save(org)
                        .doOnSuccess(updatedOrg -> {
                            eventPublisher.publishEvent(new AuditEvent(
                                "UPDATE_ORG",
                                "ORGANIZATION",
                                "Updated organization: " + updatedOrg.getName()
                            ));
                        });
                } else {
                    return Mono.error(new IllegalArgumentException("org must not be null"));
                }
            })
            .map(orgMapper::toDto)
            .switchIfEmpty(Mono.error(new RuntimeException("Organization not found")));
    }
   
/**
 * Vérifie si une organisation peut ajouter une ressource (AGENCY, VEHICLE, DRIVER)
 * selon les limites de son plan de souscription.
 */
public Mono<Boolean> validateQuota(UUID orgId, String resourceType) {
    return organizationRepository.findById(orgId)
        .flatMap(org -> planRepository.findById(org.getSubscriptionPlanId())
            .map(plan -> {
                return switch (resourceType.toUpperCase()) {
                    case "AGENCY" -> org.getCurrentAgencies() < plan.getMaxAgencies();
                    case "VEHICLE" -> org.getCurrentVehicles() < plan.getMaxVehicles();
                    case "DRIVER" -> org.getCurrentDrivers() < plan.getMaxDrivers();
                    case "USER" -> org.getCurrentUsers() < plan.getMaxUsers();
                    default -> false;
                };
            }))
        .defaultIfEmpty(false);
}

/**
 * Incrémente ou décrémente le compteur d'agences
 */
@Transactional
public Mono<Void> updateAgencyCounter(UUID orgId, int increment) {
    return organizationRepository.findById(orgId)
        .flatMap(org -> {
            org.setCurrentAgencies(org.getCurrentAgencies() + increment);
            return organizationRepository.save(org);
        }).then();
}

/**
 * Méthode groupée pour mettre à jour n'importe quel compteur
 * Utile pour les véhicules et drivers qui impactent l'org globale
 */
@Transactional
public Mono<Void> updateResourceCounter(UUID orgId, String resourceType, int increment) {
    return organizationRepository.findById(orgId)
        .flatMap(org -> {
            switch (resourceType.toUpperCase()) {
                case "VEHICLE" -> org.setCurrentVehicles(org.getCurrentVehicles() + increment);
                case "DRIVER" -> org.setCurrentDrivers(org.getCurrentDrivers() + increment);
            }
            return organizationRepository.save(org);
        }).then();
}
}
