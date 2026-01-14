package com.project.apirental.modules.driver.services;

import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.driver.domain.DriverEntity;
import com.project.apirental.modules.driver.dto.DriverResponseDTO;
import com.project.apirental.modules.driver.mapper.DriverMapper;
import com.project.apirental.modules.driver.repository.DriverRepository;
import com.project.apirental.modules.media.domain.MediaEntity;
import com.project.apirental.modules.media.services.MediaService;
import com.project.apirental.modules.organization.services.OrganizationService;
import com.project.apirental.modules.driver.dto.DriverDetailResponseDTO;
import com.project.apirental.modules.driver.dto.UpdateDriverStatusDTO;
import com.project.apirental.modules.pricing.domain.PricingEntity;
import com.project.apirental.modules.pricing.services.PricingService;
import com.project.apirental.modules.schedule.services.ScheduleService;
import com.project.apirental.shared.enums.ResourceType;
import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final AgencyRepository agencyRepository;
    private final OrganizationService organizationService;
    private final MediaService mediaService;
    private final DriverMapper driverMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduleService scheduleService;
    private final PricingService pricingService;
    /**
     * Création d'un conducteur avec Upload de fichiers et vérification de Quota
     */
    @Transactional
    public Mono<DriverResponseDTO> createDriver(
            UUID orgId,
            UUID agencyId,
            String firstname, String lastname, String tel, Integer age, Integer gender,
            FilePart profilFile, FilePart cniFile, FilePart licenseFile) {

        // 1. Vérification du Quota Conducteur (DRIVER)
        return organizationService.validateQuota(orgId, "DRIVER")
            .flatMap(hasQuota -> {
                if (!hasQuota) {
                    return Mono.error(new RuntimeException("Quota de chauffeurs atteint pour votre plan."));
                }

                // 2. Upload des fichiers en parallèle
                Mono<String> profilUrlMono = mediaService.uploadFile(profilFile).map(MediaEntity::getFileUrl);
                Mono<String> cniUrlMono = mediaService.uploadFile(cniFile).map(MediaEntity::getFileUrl);
                Mono<String> licenseUrlMono = mediaService.uploadFile(licenseFile).map(MediaEntity::getFileUrl);

                return Mono.zip(profilUrlMono, cniUrlMono, licenseUrlMono)
                    .flatMap(tuple -> {
                        // 3. Construction de l'entité
                        DriverEntity driver = DriverEntity.builder()
                                .id(UUID.randomUUID())
                                .organizationId(orgId)
                                .agencyId(agencyId)
                                .firstname(firstname)
                                .lastname(lastname)
                                .tel(tel)
                                .age(age)
                                .gender(gender)
                                .profilUrl(tuple.getT1())
                                .cniUrl(tuple.getT2())
                                .drivingLicenseUrl(tuple.getT3())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .isNewRecord(true)
                                .build();

                        // 4. Sauvegarde et mise à jour des compteurs
                        return driverRepository.save(driver).flatMap(saved -> {
                            if (saved != null) {
                                return organizationService.updateDriverCounter(orgId, 1)
                                        .then(updateAgencyDriverStats(agencyId, 1))
                                        .thenReturn(saved);
                            } else {
                                return Mono.error(new RuntimeException("Failed to save driver"));
                            }
                        });
                    });
            })
            .doOnSuccess(d -> eventPublisher.publishEvent(new AuditEvent("CREATE_DRIVER", "DRIVER", "Conducteur créé : " + d.getFirstname() + " " + d.getLastname())))
            .map(driverMapper::toDto);
    }

    public Flux<DriverResponseDTO> getDriversByOrg(UUID orgId) {
        return driverRepository.findAllByOrganizationId(orgId)
                .map(driverMapper::toDto);
    }

    /**
     * Récupère le détail complet d'un chauffeur (Info + Prix + Planning)
     */
    public Mono<DriverDetailResponseDTO> getDriverDetails(UUID id) {
        return driverRepository.findById(Objects.requireNonNull(id))
            .switchIfEmpty(Mono.error(new RuntimeException("Driver not found")))
            .flatMap(driver -> {
                var dto = driverMapper.toDto(driver);
                var pricingMono = pricingService.getPricing(ResourceType.DRIVER, id);
                var scheduleFlux = scheduleService.getResourceSchedule(ResourceType.DRIVER, id).collectList();

                return Mono.zip(pricingMono.defaultIfEmpty(new PricingEntity()), scheduleFlux)
                    .map(tuple -> new DriverDetailResponseDTO(dto, tuple.getT1().getId() == null ? null : tuple.getT1(), tuple.getT2()));
            });
    }

    public Flux<DriverResponseDTO> getDriversByAgency(UUID agencyId) {
        return driverRepository.findAllByAgencyId(agencyId)
                .map(driverMapper::toDto);
    }

    public Mono<DriverResponseDTO> getDriverById(UUID id) {
        return driverRepository.findById(Objects.requireNonNull(id))
                .map(driverMapper::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Conducteur non trouvé")));
    }

    /**
     * Changement d'agence pour un chauffeur
     */
    @Transactional
    public Mono<DriverResponseDTO> changeAgency(UUID driverId, UUID newAgencyId) {
        return driverRepository.findById(Objects.requireNonNull(driverId))
                .flatMap(driver -> {
                    UUID oldAgencyId = driver.getAgencyId();
                    if (oldAgencyId.equals(newAgencyId)) {
                        return Mono.just(driver);
                    }

                    driver.setAgencyId(newAgencyId);
                    driver.setUpdatedAt(LocalDateTime.now());

                    // Mise à jour des compteurs (On retire de l'ancienne, on ajoute à la nouvelle)
                    return updateAgencyDriverStats(oldAgencyId, -1)
                            .then(updateAgencyDriverStats(newAgencyId, 1))
                            .then(driverRepository.save(driver));
                })
                .map(driverMapper::toDto);
    }

    /**
     * Mise à jour complexe : Statut Global + Ajout Indisponibilité (Schedule) + Prix
     */
    @Transactional
    public Mono<DriverDetailResponseDTO> updateDriverStatusAndPricing(UUID id, UpdateDriverStatusDTO request) {
        return driverRepository.findById(Objects.requireNonNull(id))
            .flatMap(driver -> {
                // 1. Mise à jour statut global immédiat (ex: le chauffeur est malade MAINTENANT)
                if (request.globalStatus() != null) {
                    driver.setStatus(request.globalStatus());
                }

                // 2. Gestion du Prix
                Mono<Void> pricingMono = Mono.empty();
                if (request.pricePerHour() != null || request.pricePerDay() != null) {
                    pricingMono = pricingService.setPricing(
                        driver.getOrganizationId(),
                        ResourceType.DRIVER,
                        driver.getId(),
                        request.pricePerHour(),
                        request.pricePerDay()
                    ).then();
                }

                // 3. Gestion du Planning (Indisponibilité)
                Mono<Void> scheduleMono = Mono.empty();
                if (request.schedule() != null) {
                    scheduleMono = scheduleService.addUnavailability(
                        driver.getOrganizationId(),
                        ResourceType.DRIVER,
                        driver.getId(),
                        request.schedule()
                    ).then();
                }

                return driverRepository.save(driver)
                    .then(pricingMono)
                    .then(scheduleMono)
                    .thenReturn(driver);
            })
            .flatMap(driver -> getDriverDetails(driver.getId())); // Retourne l'objet enrichi
    }

    @Transactional
    public Mono<Void> deleteDriver(UUID id) {
        return driverRepository.findById(Objects.requireNonNull(id))
                .flatMap(driver -> driverRepository.delete(Objects.requireNonNull(driver))
                        .then(organizationService.updateDriverCounter(driver.getOrganizationId(), -1))
                        .then(updateAgencyDriverStats(driver.getAgencyId(), -1)));
    }

    // Helper pour mettre à jour les stats de l'agence
    private Mono<Void> updateAgencyDriverStats(UUID agencyId, int increment) {
        return agencyRepository.findById(Objects.requireNonNull(agencyId))
                .flatMap(agency -> {
                    agency.setTotalDrivers(agency.getTotalDrivers() + increment);
                    agency.setActiveDrivers(agency.getActiveDrivers() + increment);
                    return agencyRepository.save(agency);
                }).then();
    }
}
