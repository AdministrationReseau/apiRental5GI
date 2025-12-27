package com.project.apirental.modules.agency.mapper;

import com.project.apirental.modules.agency.domain.AgencyEntity;
import com.project.apirental.modules.agency.dto.AgencyResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AgencyMapper {
    public AgencyResponseDTO toDto(AgencyEntity entity) {
        if (entity == null) return null;
        return new AgencyResponseDTO(
            entity.getId(),
            entity.getOrganizationId(),
            entity.getName(),
            entity.getAddress(),
            entity.getCity(),
            entity.getEmail(),
            entity.getPhone(),
            entity.getActiveVehicles(),
            entity.getTotalVehicles(),
            entity.getLogoUrl()
        );
    }
}