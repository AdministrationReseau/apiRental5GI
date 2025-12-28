package com.project.apirental.modules.staff.dto;

import java.util.UUID;

public record StaffUpdateDTO(
    UUID agencyId,
    UUID posteId,
    String status // ACTIVE, INACTIVE, SUSPENDED
) {}