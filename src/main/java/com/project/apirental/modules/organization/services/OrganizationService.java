package com.project.apirental.modules.organization.services;

import com.project.apirental.modules.media.domain.MediaEntity;
import com.project.apirental.modules.media.services.MediaService;
import com.project.apirental.modules.organization.dto.OrgResponseDTO;
import com.project.apirental.modules.organization.dto.OrgUpdateDTO;
import com.project.apirental.modules.organization.mapper.OrgMapper;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository planRepository;
    private final OrgMapper orgMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaService mediaService;

    public Mono<OrgResponseDTO> getOrganization(UUID id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("id must not be null"));
        }
        return organizationRepository.findById(id)
                .map(orgMapper::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Organization not found")));
    }

    // Nouvelle méthode pour récupérer toutes les organisations (Admin)
    public Flux<OrgResponseDTO> getAllOrganizations() {
        return organizationRepository.findAll()
                .map(orgMapper::toDto);
    }

    @Transactional
    public Mono<OrgResponseDTO> updateOrganization(UUID id, OrgUpdateDTO request) {
        // Redirection vers la méthode avec fichiers nuls pour garder la compatibilité
        // si besoin
        // ou conserver la logique JSON simple existante. Ici je garde la logique
        // existante JSON-only.
        if (id == null) {
            return Mono.error(new IllegalArgumentException("id must not be null"));
        }
        return organizationRepository.findById(id)
                .flatMap(org -> {
                    if (request.name() != null)
                        org.setName(request.name());
                    if (request.description() != null)
                        org.setDescription(request.description());
                    if (request.address() != null)
                        org.setAddress(request.address());
                    if (request.city() != null)
                        org.setCity(request.city());
                    if (request.postalCode() != null)
                        org.setPostalCode(request.postalCode());
                    if (request.region() != null)
                        org.setRegion(request.region());
                    if (request.phone() != null)
                        org.setPhone(request.phone());
                    if (request.email() != null)
                        org.setEmail(request.email());
                    if (request.website() != null)
                        org.setWebsite(request.website());
                    if (request.timezone() != null)
                        org.setTimezone(request.timezone());
                    if (request.logoUrl() != null)
                        org.setLogoUrl(request.logoUrl());
                    if (request.registrationNumber() != null)
                        org.setRegistrationNumber(request.registrationNumber());
                    if (request.taxNumber() != null)
                        org.setTaxNumber(request.taxNumber());

                    return organizationRepository.save(Objects.requireNonNull(org))
                            .doOnSuccess(updatedOrg -> {
                                eventPublisher.publishEvent(new AuditEvent(
                                        "UPDATE_ORG",
                                        "ORGANIZATION",
                                        "Updated organization: " + updatedOrg.getName()));
                            });
                })
                .map(orgMapper::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Organization not found")));
    }

    /**
     * Mise à jour avancée prenant en charge les fichiers Multipart (Logo,
     * Documents)
     */
    @Transactional
    public Mono<OrgResponseDTO> updateOrganizationWithMedia(UUID id, OrgUpdateDTO request, FilePart logoFile,
            FilePart licenseFile) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("id must not be null"));
        }
        return organizationRepository.findById(id)
                .flatMap(org -> {
                    // 1. Gestion Logo : Si fichier présent -> Upload, sinon -> garder URL existante
                    Mono<String> logoMono = (logoFile != null)
                            ? mediaService.uploadFile(logoFile).map(MediaEntity::getFileUrl)
                            : Mono.justOrEmpty(org.getLogoUrl());

                    // 2. Gestion Business License
                    Mono<String> licenseMono = (licenseFile != null)
                            ? mediaService.uploadFile(licenseFile).map(MediaEntity::getFileUrl)
                            : Mono.justOrEmpty(org.getBusinessLicense());

                    // 3. Exécution parallèle des uploads (si nécessaires)
                    return Mono.zip(logoMono.defaultIfEmpty(""), licenseMono.defaultIfEmpty(""))
                            .flatMap(tuple -> {
                                String newLogoUrl = tuple.getT1();
                                String newLicenseUrl = tuple.getT2();

                                // Mise à jour des champs texte (si non nuls dans la requête)
                                if (request.name() != null)
                                    org.setName(request.name());
                                if (request.description() != null)
                                    org.setDescription(request.description());
                                if (request.phone() != null)
                                    org.setPhone(request.phone());
                                if (request.email() != null)
                                    org.setEmail(request.email());
                                if (request.address() != null)
                                    org.setAddress(request.address());
                                if (request.city() != null)
                                    org.setCity(request.city());
                                if (request.postalCode() != null)
                                    org.setPostalCode(request.postalCode());
                                if (request.region() != null)
                                    org.setRegion(request.region());
                                if (request.website() != null)
                                    org.setWebsite(request.website());
                                if (request.timezone() != null)
                                    org.setTimezone(request.timezone());
                                if (request.registrationNumber() != null)
                                    org.setRegistrationNumber(request.registrationNumber());
                                if (request.taxNumber() != null)
                                    org.setTaxNumber(request.taxNumber());

                                // Mise à jour des URLs seulement si on a une valeur (nouvelle ou ancienne
                                // récupérée)
                                if (!newLogoUrl.isEmpty())
                                    org.setLogoUrl(newLogoUrl);
                                if (!newLicenseUrl.isEmpty())
                                    org.setBusinessLicense(newLicenseUrl);

                                return organizationRepository.save(Objects.requireNonNull(org));
                            });
                })
                .doOnSuccess(updatedOrg -> eventPublisher.publishEvent(new AuditEvent(
                        "UPDATE_ORG_MEDIA",
                        "ORGANIZATION",
                        "Updated organization with media: " + updatedOrg.getName())))
                .map(orgMapper::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Organization not found")));
    }

    /**
     * Incrémente ou décrémente le compteur d'agences
     */
    @Transactional
    public Mono<Void> updateAgencyCounter(UUID orgId, int increment) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .flatMap(org -> {
                    org.setCurrentAgencies(org.getCurrentAgencies() + increment);
                    return organizationRepository.save(org);
                }).then();
    }

    /**
     * Incrémente ou décrémente le compteur de staff (Users)
     * Note : Ici maxUsers du plan limite currentUsers de l'organisation
     */
    @Transactional
    public Mono<Void> updateStaffCounter(UUID orgId, int increment) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .flatMap(org -> {
                    org.setCurrentUsers(org.getCurrentUsers() + increment);
                    return organizationRepository.save(org);
                }).then();
    }

    /**
     * Incrémente ou décrémente le compteur de véhicules
     */
    @Transactional
    public Mono<Void> updateVehicleCounter(UUID orgId, int increment) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .flatMap(org -> {
                    org.setCurrentVehicles(org.getCurrentVehicles() + increment);
                    return organizationRepository.save(org);
                }).then();
    }

    /**
     * Incrémente ou décrémente le compteur de chauffeurs
     */
    @Transactional
    public Mono<Void> updateDriverCounter(UUID orgId, int increment) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .flatMap(org -> {
                    org.setCurrentDrivers(org.getCurrentDrivers() + increment);
                    return organizationRepository.save(org);
                }).then();
    }

    /**
     * Validation des quotas (toujours centralisée pour éviter la répétition de
     * logique)
     */
    public Mono<Boolean> validateQuota(UUID orgId, String resourceType) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .map(plan -> switch (resourceType.toUpperCase()) {
                            case "AGENCY" -> org.getCurrentAgencies() < plan.getMaxAgencies();
                            case "VEHICLE" -> org.getCurrentVehicles() < plan.getMaxVehicles();
                            case "DRIVER" -> org.getCurrentDrivers() < plan.getMaxDrivers();
                            case "STAFF", "USER" -> org.getCurrentUsers() < plan.getMaxUsers();
                            default -> false;
                        }))
                .defaultIfEmpty(false);
    }
}
