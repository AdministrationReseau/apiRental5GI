package com.project.apirental.modules.vehicle.dto;

import com.project.apirental.modules.pricing.domain.PricingEntity;
import com.project.apirental.modules.review.dto.ReviewResponseDTO;
import com.project.apirental.modules.schedule.domain.ScheduleEntity;
import java.util.List;

public record VehicleDetailResponseDTO(
    VehicleResponseDTO vehicle,
    PricingEntity pricing,
    List<ScheduleEntity> schedule,
    Double rating,
    List<ReviewResponseDTO> reviews,
    Boolean isDriverBookingRequired
) {}
