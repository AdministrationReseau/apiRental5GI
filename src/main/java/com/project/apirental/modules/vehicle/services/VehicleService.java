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
    private final ObjectMapper objectMapper;

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
                            try {
                                // Conversion des objets DTO en JSON pour la BDD
                                Json functionalitiesJson = Json
                                        .of(objectMapper.writeValueAsString(request.functionalities()));
                                Json engineJson = Json.of(objectMapper.writeValueAsString(request.engineDetails()));
                                Json fuelEfficiencyJson = Json.of(objectMapper.writeValueAsString(request.fuelEfficiency()));
                                Json insuranceJson = Json.of(objectMapper.writeValueAsString(request.insuranceDetails()));
                                Json descJson = Json.of(objectMapper.writeValueAsString(request.description()));
                                Json imgsJson = Json.of(objectMapper.writeValueAsString(request.images()));

                                VehicleEntity vehicle = VehicleEntity.builder()
                                        .id(UUID.randomUUID())
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
                                        // Injection JSONB
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
                            } catch (Exception e) {
                                return Mono.error(new RuntimeException("Erreur de sérialisation des données véhicule"));
                            }

                        }))
                // 4. ENRICHISSEMENT ET MAPPING VERS DTO (C'est ici que le lien se fait)
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
    // Dans VehicleService.java

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
                        // Conversion des objets DTO en JSON pour la BDD
                        Json functionalitiesJson = Json
                                .of(objectMapper.writeValueAsString(request.functionalities()));
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