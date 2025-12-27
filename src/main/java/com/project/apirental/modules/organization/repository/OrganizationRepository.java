package com.project.apirental.modules.organization.repository;

import com.project.apirental.modules.organization.domain.OrganizationEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface OrganizationRepository extends R2dbcRepository<OrganizationEntity, UUID> {
    Flux<OrganizationEntity> findAllBySubscriptionPlanId(UUID subscriptionPlanId);
    Mono<OrganizationEntity> findByOwnerId(UUID ownerId);
}
