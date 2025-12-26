package com.project.apirental.modules.organization.services;

import com.project.apirental.modules.organization.dto.OrgResponseDTO;
import com.project.apirental.modules.organization.dto.OrgUpdateDTO;
import com.project.apirental.modules.organization.mapper.OrgMapper;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
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

    // On réutilise le repository défini dans le module Auth (ou on pourrait le déplacer dans Shared)
    private final OrganizationRepository organizationRepository;
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
                if (org != null) {
                    // Mise à jour des champs
                    if(request.name() != null) org.setName(request.name());
                    // Ici on pourrait mapper d'autres champs si l'entité OrganizationEntity avait "description" etc.

                    return organizationRepository.save(org)
                        .doOnSuccess(updatedOrg -> {
                            // Déclenchement de l'audit asynchrone
                            eventPublisher.publishEvent(new AuditEvent(
                                "UPDATE_ORG",
                                "ORGANIZATION",
                                "Updated organization: " + updatedOrg.getName()
                            ));
                        });
                } else {
                    return Mono.error(new RuntimeException("Organization not found"));
                }
            })
            .map(orgMapper::toDto)
            .switchIfEmpty(Mono.error(new RuntimeException("Organization not found")));
    }
}
