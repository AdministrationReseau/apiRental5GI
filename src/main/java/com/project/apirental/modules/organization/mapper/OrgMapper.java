package com.project.apirental.modules.organization.mapper;

import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.organization.dto.OrgResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class OrgMapper {

    public OrgResponseDTO toDto(OrganizationEntity entity) {
        if (entity == null) return null;
        return new OrgResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getOwnerId()
        );
    }
}
