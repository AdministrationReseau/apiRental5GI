package com.project.apirental.modules.organization.dto;

import com.project.apirental.modules.auth.domain.UserEntity;

public record OrgUserResponseDTO(
    UserEntity user,
    OrgResponseDTO organization
) {}
