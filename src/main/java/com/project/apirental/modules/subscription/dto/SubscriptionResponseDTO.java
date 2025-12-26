package com.project.apirental.modules.subscription.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionResponseDTO(
    UUID id,
    UUID organizationId,
    String planType,
    String status,
    LocalDateTime startDate,
    LocalDateTime endDate
) {}