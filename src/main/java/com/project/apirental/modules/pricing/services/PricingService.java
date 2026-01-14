package com.project.apirental.modules.pricing.services;

import com.project.apirental.modules.pricing.domain.PricingEntity;
import com.project.apirental.modules.pricing.repository.PricingRepository;
import com.project.apirental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingService {
    private final PricingRepository pricingRepository;

    public Mono<PricingEntity> setPricing(UUID orgId, ResourceType type, UUID resourceId, BigDecimal perHour, BigDecimal perDay) {
        return pricingRepository.findByResourceTypeAndResourceId(type, resourceId)
            .defaultIfEmpty(PricingEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .resourceType(type)
                .resourceId(resourceId)
                .isNewRecord(true)
                .createdAt(LocalDateTime.now())
                .build())
            .flatMap(pricing -> {
                pricing.setPricePerHour(perHour);
                pricing.setPricePerDay(perDay);
                pricing.setUpdatedAt(LocalDateTime.now());
                return pricingRepository.save(pricing);
            });
    }

    public Mono<PricingEntity> getPricing(ResourceType type, UUID resourceId) {
        return pricingRepository.findByResourceTypeAndResourceId(type, resourceId);
    }
}
