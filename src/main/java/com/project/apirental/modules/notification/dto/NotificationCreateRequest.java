package com.project.apirental.modules.notification.dto;

import java.util.UUID;

/**
 * DTO pour créer une notification
 */
public record NotificationCreateRequest(
        UUID locationId,
        UUID resourceId,
        String resourceType,        // CLIENT, DRIVER, AGENCY
        String reason,              // RESERVATION, LOCATION_START, LOCATION_END
        UUID vehicleId,
        UUID driverId,
        String details
) {}
