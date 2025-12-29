package com.project.apirental.modules.agency.dto;

import java.util.UUID;

public record AgencyResponseDTO(
    UUID id,
    UUID organizationId,
    String name,
    String description,
    String address,
    String city,
    String country,
    String phone,
    String email,
    UUID managerId,
    Double latitude,
    Double longitude,
    Boolean is24Hours,
    String timezone,
    String workingHours,
    Boolean allowOnlineBooking,
    Double depositPercentage,
    String logoUrl
) {}