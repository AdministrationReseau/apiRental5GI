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
}
