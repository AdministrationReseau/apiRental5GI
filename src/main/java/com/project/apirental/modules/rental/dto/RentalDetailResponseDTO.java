package com.project.apirental.modules.rental.dto;

import com.project.apirental.modules.agency.dto.AgencyResponseDTO;
import com.project.apirental.modules.driver.dto.DriverResponseDTO;
import com.project.apirental.modules.rental.domain.RentalEntity;
import com.project.apirental.modules.vehicle.dto.VehicleResponseDTO;

public record RentalDetailResponseDTO(
    RentalEntity rental,
    VehicleResponseDTO vehicle,
    DriverResponseDTO driver, // Peut être null si aucun chauffeur n'est sélectionné
    AgencyResponseDTO agency
) {}
