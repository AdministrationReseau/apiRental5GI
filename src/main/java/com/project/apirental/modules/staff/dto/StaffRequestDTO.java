package com.project.apirental.modules.staff.dto;

import java.util.UUID;

public record StaffRequestDTO(
    String userEmail, // On cherche l'utilisateur par son email
    UUID agencyId,
    UUID posteId
) {}