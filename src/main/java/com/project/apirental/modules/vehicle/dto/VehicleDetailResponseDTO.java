package com.project.apirental.modules.vehicle.dto;

import com.project.apirental.modules.pricing.domain.PricingEntity;
import com.project.apirental.modules.schedule.domain.ScheduleEntity;
import java.util.List;

public record VehicleDetailResponseDTO(
    VehicleResponseDTO vehicle,
    PricingEntity pricing, // Prix spécifique
    List<ScheduleEntity> schedule
) {}
