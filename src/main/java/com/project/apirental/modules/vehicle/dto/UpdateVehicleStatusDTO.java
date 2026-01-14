package com.project.apirental.modules.vehicle.dto;

import com.project.apirental.shared.dto.ScheduleRequestDTO;
import java.math.BigDecimal;

public record UpdateVehicleStatusDTO(
    String globalStatus, // AVAILABLE, MAINTENANCE, UNAVAILABLE
    ScheduleRequestDTO schedule, // Période d'indisponibilité (ex: Maintenance du 10 au 12)
    BigDecimal pricePerHour,
    BigDecimal pricePerDay
) {}
