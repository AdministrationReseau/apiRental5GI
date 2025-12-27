package com.project.apirental.modules.agency.repository;

import com.project.apirental.modules.agency.domain.AgencyEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

public interface AgencyRepository extends R2dbcRepository<AgencyEntity, UUID> {
    Flux<AgencyEntity> findAllByOrganizationId(UUID organizationId);
}