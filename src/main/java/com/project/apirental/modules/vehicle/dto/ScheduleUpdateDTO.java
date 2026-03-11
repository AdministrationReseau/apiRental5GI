package com.project.apirental.modules.vehicle.dto;

import com.project.apirental.shared.dto.ScheduleRequestDTO;
import java.util.List;

public record ScheduleUpdateDTO(
    List<ScheduleRequestDTO> schedules
) {}
