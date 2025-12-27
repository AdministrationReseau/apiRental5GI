package com.project.apirental.modules.organization.repository;

import com.project.apirental.modules.organization.domain.OrganizationEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface OrganizationRepository extends R2dbcRepository<OrganizationEntity, UUID> {
<<<<<<< HEAD
    Flux<OrganizationEntity> findAllBySubscriptionPlanId(UUID subscriptionPlanId);
=======
    Flux<OrganizationEntity> findAllBySubscriptionPlanName(String subscriptionPlanNam);
    Mono<OrganizationEntity> findByOwnerId(UUID ownerId);
>>>>>>> cc9b34474bd7983d0d492a39b6d9831f69f29be3
}
