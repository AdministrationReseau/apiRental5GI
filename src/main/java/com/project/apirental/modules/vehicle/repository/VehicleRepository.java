package com.project.apirental.modules.vehicle.repository;

import com.project.apirental.modules.vehicle.domain.VehicleEntity;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface VehicleRepository extends R2dbcRepository<VehicleEntity, UUID> {
    Flux<VehicleEntity> findAllByAgencyId(UUID agencyId);
    Flux<VehicleEntity> findAllByOrganizationId(UUID organizationId);
    @Query("SELECT organization_id FROM vehicles WHERE id = :vehicleId")
    Mono<UUID> findOrgIdByVehicleId(UUID vehicleId);
}