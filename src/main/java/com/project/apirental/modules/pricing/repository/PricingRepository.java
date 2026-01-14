package com.project.apirental.modules.pricing.repository;

import com.project.apirental.modules.pricing.domain.PricingEntity;
import com.project.apirental.shared.enums.ResourceType;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface PricingRepository extends R2dbcRepository<PricingEntity, UUID> {
    Mono<PricingEntity> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);
}
