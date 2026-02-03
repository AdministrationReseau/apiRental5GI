package com.project.apirental.modules.vehicle.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
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
    private final ObjectMapper objectMapper;

    @Transactional
    public Mono<VehicleResponseDTO> createVehicle(UUID orgId, VehicleRequestDTO request) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .switchIfEmpty(Mono.<OrganizationEntity>error(new RuntimeException("Organisation non trouvée")))
                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .flatMap(plan -> {

                            if (org.getCurrentVehicles() >= plan.getMaxVehicles()) {
                                return Mono.<VehicleEntity>error(new RuntimeException(
                                        "Quota de véhicules atteint pour votre plan (" + plan.getName() + ")"));
                            }

                            try {
                                Json functionalitiesJson = Json.of(objectMapper.writeValueAsString(request.functionalities()));
                                Json engineJson = Json.of(objectMapper.writeValueAsString(request.engineDetails()));
                                Json fuelEfficiencyJson = Json.of(objectMapper.writeValueAsString(request.fuelEfficiency()));
                                Json insuranceJson = Json.of(objectMapper.writeValueAsString(request.insuranceDetails()));
                                Json descJson = Json.of(objectMapper.writeValueAsString(request.description()));
                                Json imgsJson = Json.of(objectMapper.writeValueAsString(request.images()));

                                VehicleEntity vehicle = VehicleEntity.builder()
                                        .id(UUID.randomUUID()) // Génération explicite de l'ID
                                        .organizationId(orgId)
                                        .agencyId(request.agencyId())
                                        .categoryId(request.categoryId())
                                        .licencePlate(request.licencePlate())
                                        .vinNumber(request.vinNumber())
                                        .brand(request.brand())
                                        .model(request.model())
                                        .yearProduction(request.yearProduction())
                                        .places(request.places())
                                        .kilometrage(request.kilometrage())
                                        .statut(request.statut())
                                        .color(request.color())
                                        .transmission(request.transmission())
                                        .functionalities(functionalitiesJson)
                                        .engineDetails(engineJson)
                                        .fuelEfficiency(fuelEfficiencyJson)
                                        .insuranceDetails(insuranceJson)
                                        .descriptionList(descJson)
                                        .imagesList(imgsJson)
                                        .createdAt(LocalDateTime.now())
                                        .statut("AVAILABLE")
                                        .isNewRecord(true)
                                        .build();

                                return vehicleRepository.save(Objects.requireNonNull(vehicle))
                                        .flatMap(savedVehicle -> {
                                            org.setCurrentVehicles(org.getCurrentVehicles() + 1);
                                            return organizationRepository.save(org)
                                                    .then(updateAgencyVehicleStats(request.agencyId(), 1))
                                                    .thenReturn(savedVehicle);
                                        });
                            } catch (Exception e) {
                                return Mono.error(new RuntimeException("Erreur de sérialisation des données véhicule"));
                            }

                        }))
                .flatMap(savedVehicle -> categoryRepository
                        .findById(Objects.requireNonNull(savedVehicle.getCategoryId()))
                        .map(cat -> vehicleMapper.toDto(savedVehicle, cat))
                        .defaultIfEmpty(vehicleMapper.toDto(savedVehicle, null)))
                .doOnSuccess(v -> eventPublisher.publishEvent(
                        new AuditEvent("CREATE_VEHICLE", "VEHICLE", "Véhicule ajouté : " + v.licencePlate())));
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
                Mono<PricingEntity> pricingMono = pricingService.getPricing(ResourceType.VEHICLE, id);
                var scheduleFlux = scheduleService.getResourceSchedule(ResourceType.VEHICLE, id).collectList();
                Mono<VehicleResponseDTO> vehicleDtoMono = enrichVehicle(vehicle);

                return Mono.zip(vehicleDtoMono, pricingMono.defaultIfEmpty(new PricingEntity()), scheduleFlux)
                    .map(tuple -> new VehicleDetailResponseDTO(tuple.getT1(), tuple.getT2().getId() == null ? null : tuple.getT2(), tuple.getT3()));
            });
    }

    @Transactional
    public Mono<VehicleDetailResponseDTO> updateVehicleStatusAndPricing(UUID id, UpdateVehicleStatusDTO request) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
            .flatMap(vehicle -> {
                if (request.globalStatus() != null) {
                    vehicle.setStatut(request.globalStatus());
                }
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

    /**
     * Lister tous les véhicules disponibles sur la plateforme (statut = AVAILABLE)
     */
    public Flux<VehicleResponseDTO> getAvailableVehicles() {
        return vehicleRepository.findAllByStatut("AVAILABLE")
                .flatMap(this::enrichVehicle);
    }

    public Mono<VehicleResponseDTO> getVehicleById(UUID id) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
                .flatMap(this::enrichVehicle);
    }

    @Transactional
    public Mono<VehicleResponseDTO> updateVehicle(UUID id, VehicleRequestDTO request) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
            .switchIfEmpty(Mono.error(new RuntimeException("Véhicule non trouvé")))
                .flatMap(v -> {
                    v.setAgencyId(request.agencyId());
                    v.setCategoryId(request.categoryId());
                    v.setLicencePlate(request.licencePlate());
                    v.setVinNumber(request.vinNumber());
                    v.setBrand(request.brand());
                    v.setModel(request.model());
                    v.setYearProduction(request.yearProduction());
                    v.setPlaces(request.places());
                    v.setKilometrage(request.kilometrage());
                    v.setStatut(request.statut());
                    v.setColor(request.color());
                    v.setTransmission(request.transmission());
                    try {
                        Json functionalitiesJson = Json.of(objectMapper.writeValueAsString(request.functionalities()));
                        Json engineJson = Json.of(objectMapper.writeValueAsString(request.engineDetails()));
                        Json fuelEfficiencyJson = Json.of(objectMapper.writeValueAsString(request.fuelEfficiency()));
                        Json insuranceJson = Json.of(objectMapper.writeValueAsString(request.insuranceDetails()));
                        Json descJson = Json.of(objectMapper.writeValueAsString(request.description()));
                        Json imgsJson = Json.of(objectMapper.writeValueAsString(request.images()));

                        v.setFunctionalities(functionalitiesJson);
                        v.setEngineDetails(engineJson);
                        v.setFuelEfficiency(fuelEfficiencyJson);
                        v.setInsuranceDetails(insuranceJson);
                        v.setDescriptionList(descJson);
                        v.setImagesList(imgsJson);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Erreur de sérialisation des données véhicule"));
                    }
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

    public Flux<VehicleResponseDTO> getVehiclesByOrgAndCategory(UUID orgId, UUID categoryId) {
        return vehicleRepository.findAllByOrganizationIdAndCategoryId(orgId, categoryId)
                .flatMap(this::enrichVehicle);
    }

    public Flux<VehicleResponseDTO> getVehiclesByAgencyAndCategory(UUID agencyId, UUID categoryId) {
        return vehicleRepository.findAllByAgencyIdAndCategoryId(agencyId, categoryId)
                .flatMap(this::enrichVehicle);
    }
}
