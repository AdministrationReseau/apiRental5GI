package com.project.apirental.modules.agency.repository;

import com.project.apirental.modules.agency.domain.AgencyEntity;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AgencyRepository extends R2dbcRepository<AgencyEntity, UUID> {
    Flux<AgencyEntity> findAllByOrganizationId(UUID organizationId);
    @Query("SELECT organization_id FROM agencies WHERE id = :agencyId")
    Mono<UUID> findOrgIdByAgencyId(UUID agencyId);
}