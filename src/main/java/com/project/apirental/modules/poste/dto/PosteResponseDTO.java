package com.project.apirental.modules.poste.dto;

import com.project.apirental.modules.permission.domain.PermissionEntity;
import java.util.List;
import java.util.UUID;

public record PosteResponseDTO(
    UUID id,
    String name,
    String description,
    List<PermissionEntity> permissions
) {}
