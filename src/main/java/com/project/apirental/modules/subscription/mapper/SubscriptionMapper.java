package com.project.apirental.modules.subscription.mapper;

import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.subscription.domain.SubscriptionPlanEntity;
import com.project.apirental.modules.subscription.dto.SubscriptionResponseDTO;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class SubscriptionMapper {

    public SubscriptionResponseDTO toResponseDTO(OrganizationEntity org, SubscriptionPlanEntity plan) {
        boolean isExpired = org.getSubscriptionExpiresAt() != null && org.getSubscriptionExpiresAt().isBefore(LocalDateTime.now());
        
        return new SubscriptionResponseDTO(
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getDurationDays(),
                plan.getMaxVehicles(),
                plan.getMaxAgencies(),
                org.getSubscriptionExpiresAt(),
                isExpired
        );
    }
}