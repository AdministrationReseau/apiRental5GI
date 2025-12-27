package com.project.apirental.modules.agency.dto;

import java.util.UUID;

public record AgencyResponseDTO(
    UUID id,
    UUID organizationId,
    String name,
    String address,
    String city,
    String email,
    String phone,
    Integer activeVehicles,
    Integer totalVehicles,
    String logoUrl
    // Ajoutez d'autres champs selon les besoins du frontend
) {}