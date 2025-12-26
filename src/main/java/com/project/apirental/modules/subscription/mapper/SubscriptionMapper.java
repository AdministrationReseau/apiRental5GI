package com.project.apirental.modules.subscription.mapper;

import com.project.apirental.modules.subscription.domain.SubscriptionEntity;
import com.project.apirental.modules.subscription.dto.SubscriptionResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public SubscriptionResponseDTO toDto(SubscriptionEntity entity) {
        if (entity == null) return null;
        return new SubscriptionResponseDTO(
            entity.getId(),
            entity.getOrganizationId(),
            entity.getPlanType(),
            entity.getStatus(),
            entity.getStartDate(),
            entity.getEndDate()
        );
    }
}