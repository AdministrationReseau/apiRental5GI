package com.project.apirental.modules.staff.dto;

import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.poste.dto.PosteResponseDTO;
import java.util.UUID;

public record StaffResponseDTO(
    UUID staffId,
    UserEntity user,
    UUID agencyId,
    PosteResponseDTO poste,
    String status
) {}