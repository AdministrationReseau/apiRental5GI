package com.project.apirental.modules.driver.services;

import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.driver.domain.DriverEntity;
import com.project.apirental.modules.driver.dto.DriverResponseDTO;
import com.project.apirental.modules.driver.mapper.DriverMapper;
import com.project.apirental.modules.driver.repository.DriverRepository;
import com.project.apirental.modules.media.domain.MediaEntity;
import com.project.apirental.modules.media.services.MediaService;
import com.project.apirental.modules.organization.services.OrganizationService;
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
                        return driverRepository.save(driver)
                                .flatMap(saved ->
                                    organizationService.updateDriverCounter(orgId, 1)
                                    .then(updateAgencyDriverStats(agencyId, 1))
                                    .thenReturn(saved)
                                );
                    });
            })
            .doOnSuccess(d -> eventPublisher.publishEvent(new AuditEvent("CREATE_DRIVER", "DRIVER", "Conducteur créé : " + d.getFirstname() + " " + d.getLastname())))
            .map(driverMapper::toDto);
    }

    public Flux<DriverResponseDTO> getDriversByOrg(UUID orgId) {
        return driverRepository.findAllByOrganizationId(orgId)
                .map(driverMapper::toDto);
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

    @Transactional
    public Mono<Void> deleteDriver(UUID id) {
        return driverRepository.findById(Objects.requireNonNull(id))
                .flatMap(driver -> driverRepository.delete(driver)
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
