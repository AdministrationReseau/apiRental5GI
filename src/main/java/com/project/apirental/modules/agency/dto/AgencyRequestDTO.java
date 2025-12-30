package com.project.apirental.modules.agency.dto;

import java.util.UUID;

public record AgencyRequestDTO(
    String name,
    String description,
    String address,
    String city,
    String country,
    String postalCode,
    String region,
    String phone,
    String email,
    UUID managerId,
    Double latitude,
    Double longitude,
    Double geofenceRadius,
    Boolean is24Hours,
    String timezone,
    String workingHours,
    Boolean allowOnlineBooking,
    Double depositPercentage,
    String logoUrl,
    String primaryColor,
    String secondaryColor
) {}