package com.project.apirental.modules.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pour répondre avec une notification
 */
public record NotificationResponseDTO(
        UUID id,
        UUID locationId,
        UUID resourceId,
        String resourceType,
        String reason,
        UUID vehicleId,
        UUID driverId,
        LocalDateTime createdAt,
        Boolean isRead,
        String details
) {}
