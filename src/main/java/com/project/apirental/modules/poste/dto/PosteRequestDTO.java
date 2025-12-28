package com.project.apirental.modules.poste.dto;

import java.util.List;
import java.util.UUID;

public record PosteRequestDTO(
    String name,
    String description,
    List<UUID> permissionIds // Liste des ID de permissions à lier
) {}
