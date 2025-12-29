package com.project.apirental.modules.staff.dto;

import com.project.apirental.modules.poste.dto.PosteResponseDTO;
import java.time.LocalDateTime;
import java.util.UUID;

public record StaffResponseDTO(
    UUID id,
    String firstname,
    String lastname,
    String email,
    UUID agencyId,
    PosteResponseDTO poste,
    String status,
    LocalDateTime hiredAt
) {}