package com.project.apirental.modules.staff.dto;

import java.util.UUID;

public record StaffUpdateDTO(
    String firstname,
    String lastname,
    UUID agencyId,
    UUID posteId,
    String status
) {}