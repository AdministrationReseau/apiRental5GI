package com.project.apirental.modules.driver.repository;

import com.project.apirental.modules.driver.domain.DriverEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface DriverRepository extends R2dbcRepository<DriverEntity, UUID> {
    Flux<DriverEntity> findAllByOrganizationId(UUID organizationId);
    Flux<DriverEntity> findAllByAgencyId(UUID agencyId);
}
