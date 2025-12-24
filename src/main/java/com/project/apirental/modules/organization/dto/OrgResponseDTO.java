package com.project.apirental.modules.organization.dto;

import java.util.UUID;

public record OrgResponseDTO(UUID id, String name, UUID ownerId) {}
