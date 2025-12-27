
package com.project.apirental.modules.agency.services;

import com.project.apirental.modules.agency.domain.AgencyEntity;
import com.project.apirental.modules.agency.dto.AgencyRequestDTO;
import com.project.apirental.modules.agency.dto.AgencyResponseDTO;
import com.project.apirental.modules.agency.mapper.AgencyMapper;
import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository planRepository;
    private final AgencyMapper agencyMapper;
    private final ApplicationEventPublisher eventPublisher;
    

    @Transactional
    public Mono<AgencyResponseDTO> createAgency(UUID orgId, AgencyRequestDTO request) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
            .switchIfEmpty(Mono.error(new RuntimeException("Organisation non trouvée")))
            .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                .flatMap(plan -> {
                    // 1. VERIFICATION QUOTA AGENCES
                    if (org.getCurrentAgencies() >= plan.getMaxAgencies()) {
                        return Mono.error(new RuntimeException("Quota d'agences atteint pour votre plan (" + plan.getName() + ")"));
                    }

                    // 2. PREPARATION DE L'AGENCE
                    AgencyEntity agency = AgencyEntity.builder()
                            .id(UUID.randomUUID())
                            .organizationId(orgId)
                            .name(request.name())
                            .address(request.address())
                            .city(request.city())
                            .country(request.country() != null ? request.country() : "CM")
                            .isNewRecord(true)
                            .build();

                    // 3. SAUVEGARDE + MISE A JOUR COMPTEUR ORG
                    return agencyRepository.save(Objects.requireNonNull(agency))
                            .flatMap(savedAgency -> {
                                org.setCurrentAgencies(org.getCurrentAgencies() + 1);
                                return organizationRepository.save(org)
                                        .thenReturn(savedAgency);
                            });
                }))
            .doOnSuccess(a -> eventPublisher.publishEvent(new AuditEvent("CREATE_AGENCY", "AGENCY", "Agence créée : " + a.getName())))
            .map(agencyMapper::toDto);
    }

    /**
     * Cette méthode pourra être appelée par les modules Vehicle ou Staff 
     * pour vérifier si l'organisation a encore du crédit dans son plan.
     */
    public Mono<Boolean> canAddResource(UUID orgId, String resourceType) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
            .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                .map(plan -> {
                    return switch (resourceType.toUpperCase()) {
                        case "VEHICLE" -> org.getCurrentVehicles() < plan.getMaxVehicles();
                        case "DRIVER" -> org.getCurrentDrivers() < plan.getMaxDrivers();
                        case "USER" -> org.getCurrentUsers() < plan.getMaxUsers();
                        default -> false;
                    };
                }));
    }

    public Flux<AgencyResponseDTO> getAgenciesByOrg(UUID orgId) {
        return agencyRepository.findAllByOrganizationId(orgId)
                .map(agencyMapper::toDto);
    }

    public Mono<AgencyResponseDTO> getAgency(UUID id) {
        return agencyRepository.findById(Objects.requireNonNull(id))
                .map(agencyMapper::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Agence non trouvée")));
    }


    @Transactional
    public Mono<AgencyResponseDTO> updateAgency(UUID id, AgencyRequestDTO request) {
        return agencyRepository.findById(Objects.requireNonNull(id))
                .flatMap(existing -> {
                    if(request.name() != null) existing.setName(request.name());
                    if(request.address() != null) existing.setAddress(request.address());
                    if(request.city() != null) existing.setCity(request.city());
                    if(request.phone() != null) existing.setPhone(request.phone());
                    // ... mettre à jour les autres champs nécessaires
                    return agencyRepository.save(Objects.requireNonNull(existing));
                })
                .doOnSuccess(updated -> eventPublisher.publishEvent(
                        new AuditEvent("UPDATE_AGENCY", "AGENCY", "Updated agency: " + updated.getName())
                ))
                .map(agencyMapper::toDto);
    }

    public Mono<Void> deleteAgency(UUID id) {
        return agencyRepository.deleteById(Objects.requireNonNull(id))
                .doOnSuccess(v -> eventPublisher.publishEvent(
                        new AuditEvent("DELETE_AGENCY", "AGENCY", "Deleted agency ID: " + id)
                ));
    }
}