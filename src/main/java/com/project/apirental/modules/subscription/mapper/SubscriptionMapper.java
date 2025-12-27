package com.project.apirental.modules.subscription.mapper;

import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.subscription.domain.SubscriptionCatalog;
import com.project.apirental.modules.subscription.domain.SubscriptionCatalog.SubscriptionPlan;
import com.project.apirental.modules.subscription.dto.SubscriptionResponseDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SubscriptionMapper {

    public SubscriptionResponseDTO toResponseDTO(OrganizationEntity org) {
        if (org == null) return null;

        // On récupère la définition du plan dans le catalogue statique
        SubscriptionPlan planDef = SubscriptionCatalog.PLANS.getOrDefault(
                org.getSubscriptionPlanName(), 
                SubscriptionCatalog.PLANS.get(SubscriptionCatalog.PLAN_FREE)
        );

        boolean isExpired = org.getSubscriptionExpiresAt() != null 
                && org.getSubscriptionExpiresAt().isBefore(LocalDateTime.now());

        return new SubscriptionResponseDTO(
                org.getSubscriptionPlanName(),
                planDef.description(),
                planDef.price(),
                planDef.maxVehicles(),
                planDef.maxAgencies(),
                org.getSubscriptionExpiresAt(),
                isExpired
        );
    }
}