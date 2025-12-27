package com.project.apirental.modules.subscription.repository;

import com.project.apirental.modules.subscription.domain.SubscriptionPlanEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface SubscriptionPlanRepository extends R2dbcRepository<SubscriptionPlanEntity, UUID> {
    Mono<SubscriptionPlanEntity> findByName(String name);
}