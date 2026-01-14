package com.project.apirental.modules.vehicle.services;

import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.organization.services.OrganizationService;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.modules.vehicle.domain.VehicleEntity;
import com.project.apirental.modules.vehicle.dto.VehicleRequestDTO;
import com.project.apirental.modules.vehicle.dto.VehicleResponseDTO;
import com.project.apirental.modules.vehicle.mapper.VehicleMapper;
import com.project.apirental.modules.vehicle.repository.CategoryRepository;
import com.project.apirental.modules.vehicle.repository.VehicleRepository;
import com.project.apirental.modules.vehicle.dto.VehicleDetailResponseDTO;
import com.project.apirental.modules.vehicle.dto.UpdateVehicleStatusDTO;
import com.project.apirental.modules.pricing.domain.PricingEntity;
import com.project.apirental.modules.pricing.services.PricingService;
import com.project.apirental.modules.schedule.services.ScheduleService;
import com.project.apirental.shared.enums.ResourceType;
import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final SubscriptionPlanRepository planRepository;
    private final CategoryRepository categoryRepository;
    private final VehicleMapper vehicleMapper;
    private final AgencyRepository agencyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduleService scheduleService;
    private final PricingService pricingService;

    @Transactional
    public Mono<VehicleResponseDTO> createVehicle(UUID orgId, VehicleRequestDTO request) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .switchIfEmpty(Mono.<OrganizationEntity>error(new RuntimeException("Organisation non trouvée")))
                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .flatMap(plan -> {

                            // 1. VÉRIFICATION QUOTA VÉHICULES
                            if (org.getCurrentVehicles() >= plan.getMaxVehicles()) {
                                // On précise le type <VehicleEntity> pour l'inférence
                                return Mono.<VehicleEntity>error(new RuntimeException(
                                        "Quota de véhicules atteint pour votre plan (" + plan.getName() + ")"));
                            }

                            // 2. PRÉPARATION DU VÉHICULE
                            VehicleEntity vehicle = VehicleEntity.builder()
                                    .id(UUID.randomUUID())
                                    .organizationId(orgId)
                                    .agencyId(request.agencyId())
                                    .categoryId(request.categoryId())
                                    .immatriculation(request.immatriculation())
                                    .marque(request.marque())
                                    .modele(request.modele())
                                    .kilometrage(request.kilometrage())
                                    .transmission(request.transmission())
                                    .couleur(request.couleur())
                                    .places(request.places())
                                    .hasAirConditioner(request.hasAirConditioner())
                                    .hasWifi(request.hasWifi())
                                    .gpsType(request.gpsType())
                                    .imageUrl(request.imageUrl())
                                    .statut("AVAILABLE") // Statut par défaut
                                    .createdAt(LocalDateTime.now())
                                    .isNewRecord(true)
                                    .build();

                            // 3. SAUVEGARDE ET MISE À JOUR DES COMPTEURS
                            return vehicleRepository.save(Objects.requireNonNull(vehicle))
                                    .flatMap(savedVehicle -> {
                                        // Mise à jour du compteur de l'organisation
                                        org.setCurrentVehicles(org.getCurrentVehicles() + 1);

                                        return organizationRepository.save(org)
                                                .then(updateAgencyVehicleStats(request.agencyId(), 1))
                                                // On retourne le véhicule sauvegardé pour la suite de la chaîne
                                                .thenReturn(savedVehicle);
                                    });
                        }))
                // 4. ENRICHISSEMENT ET MAPPING VERS DTO (C'est ici que le lien se fait)
                .flatMap(savedVehicle -> categoryRepository.findById(Objects.requireNonNull(savedVehicle.getCategoryId()))
                        .map(cat -> vehicleMapper.toDto(savedVehicle, cat))
                        .defaultIfEmpty(vehicleMapper.toDto(savedVehicle, null)))
                .doOnSuccess(v -> eventPublisher.publishEvent(
                        new AuditEvent("CREATE_VEHICLE", "VEHICLE", "Véhicule ajouté : " + v.immatriculation())));
    }

    private Mono<Void> updateAgencyVehicleStats(UUID agencyId, int increment) {
        return agencyRepository.findById(Objects.requireNonNull(agencyId))
                .flatMap(agency -> {
                    agency.setTotalVehicles(agency.getTotalVehicles() + increment);
                    agency.setActiveVehicles(agency.getActiveVehicles() + increment);
                    return agencyRepository.save(agency);
                }).then();
    }

    public Mono<VehicleDetailResponseDTO> getVehicleDetails(UUID id) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
            .flatMap(vehicle -> {
                // Pour le prix, on cherche d'abord le prix du véhicule, sinon celui de la catégorie
                Mono<PricingEntity> pricingMono = pricingService.getPricing(ResourceType.VEHICLE, id);

                var scheduleFlux = scheduleService.getResourceSchedule(ResourceType.VEHICLE, id).collectList();

                // On doit enrichir le DTO Vehicle de base (avec catégorie)
                Mono<VehicleResponseDTO> vehicleDtoMono = enrichVehicle(vehicle);

                return Mono.zip(vehicleDtoMono, pricingMono.defaultIfEmpty(new PricingEntity()), scheduleFlux)
                    .map(tuple -> new VehicleDetailResponseDTO(tuple.getT1(), tuple.getT2().getId() == null ? null : tuple.getT2(), tuple.getT3()));
            });
    }

    @Transactional
    public Mono<VehicleDetailResponseDTO> updateVehicleStatusAndPricing(UUID id, UpdateVehicleStatusDTO request) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
            .flatMap(vehicle -> {
                // 1. Statut global
                if (request.globalStatus() != null) {
                    vehicle.setStatut(request.globalStatus());
                }

                // 2. Prix (Spécifique au véhicule)
                Mono<Void> pricingMono = Mono.empty();
                if (request.pricePerHour() != null || request.pricePerDay() != null) {
                    pricingMono = pricingService.setPricing(
                        vehicle.getOrganizationId(),
                        ResourceType.VEHICLE,
                        vehicle.getId(),
                        request.pricePerHour(),
                        request.pricePerDay()
                    ).then();
                }

                // 3. Planning (Maintenance, etc.)
                Mono<Void> scheduleMono = Mono.empty();
                if (request.schedule() != null) {
                    scheduleMono = scheduleService.addUnavailability(
                        vehicle.getOrganizationId(),
                        ResourceType.VEHICLE,
                        vehicle.getId(),
                        request.schedule()
                    ).then();
                }

                return vehicleRepository.save(vehicle)
                    .then(pricingMono)
                    .then(scheduleMono)
                    .thenReturn(vehicle);
            })
            .flatMap(vehicle -> getVehicleDetails(vehicle.getId()));
    }

    public Flux<VehicleResponseDTO> getVehiclesByOrg(UUID orgId) {
        return vehicleRepository.findAllByOrganizationId(orgId)
                .flatMap(this::enrichVehicle);
    }

    public Flux<VehicleResponseDTO> getVehiclesByAgency(UUID agencyId) {
        return vehicleRepository.findAllByAgencyId(agencyId)
                .flatMap(this::enrichVehicle);
    }

    public Mono<VehicleResponseDTO> getVehicleById(UUID id) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
                .flatMap(this::enrichVehicle);
    }

    @Transactional
    public Mono<VehicleResponseDTO> updateVehicle(UUID id, VehicleRequestDTO request) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
                .flatMap(v -> {
                    v.setKilometrage(request.kilometrage());
                    v.setCouleur(request.couleur());
                    v.setImageUrl(request.imageUrl());
                    v.setStatut(request.statut());
                    v.setCategoryId(request.categoryId());
                    v.setImmatriculation(request.immatriculation());
                    v.setMarque(request.marque());
                    v.setModele(request.modele());
                    v.setKilometrage(request.kilometrage());
                    v.setTransmission(request.transmission());
                    v.setCouleur(request.couleur());
                    v.setPlaces(request.places());
                    v.setHasAirConditioner(request.hasAirConditioner());
                    v.setHasWifi(request.hasWifi());
                    v.setGpsType(request.gpsType());

                    return vehicleRepository.save(v);
                })
                .flatMap(this::enrichVehicle);
    }

    @Transactional
    public Mono<VehicleResponseDTO> updateVehicleStatus(UUID id, String status) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
                .flatMap(v -> {
                    v.setStatut(status.toUpperCase());
                    return vehicleRepository.save(v);
                })
                .flatMap(this::enrichVehicle);
    }

    @Transactional
    public Mono<Void> deleteVehicle(UUID id) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
                .flatMap(v -> vehicleRepository.delete(Objects.requireNonNull(v))
                        .then(organizationService.updateVehicleCounter(v.getOrganizationId(), -1))
                        .then(updateAgencyVehicleStats(v.getAgencyId(), -1)));
    }

    private Mono<VehicleResponseDTO> enrichVehicle(VehicleEntity vehicle) {
        return categoryRepository.findById(Objects.requireNonNull(vehicle.getCategoryId()))
                .map(cat -> vehicleMapper.toDto(vehicle, cat))
                .defaultIfEmpty(vehicleMapper.toDto(vehicle, null));
    }
}
