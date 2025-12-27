package com.project.apirental.modules.subscription.repository;

import com.project.apirental.modules.subscription.domain.SubscriptionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface SubscriptionRepository extends R2dbcRepository<SubscriptionEntity, UUID> {
    Mono<SubscriptionEntity> findByName(String name);
}