package com.project.apirental.modules.driver.dto;

import com.project.apirental.modules.pricing.domain.PricingEntity;
import com.project.apirental.modules.schedule.domain.ScheduleEntity;
import java.util.List;

public record DriverDetailResponseDTO(
    DriverResponseDTO driver,
    PricingEntity pricing,
    List<ScheduleEntity> schedule
) {}
