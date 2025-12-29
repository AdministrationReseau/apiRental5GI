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
            entity.getDescription(),
            entity.getAddress(),
            entity.getCity(),
            entity.getCountry(),            
            entity.getPhone(),
            entity.getEmail(),
            entity.getManagerId(),
            entity.getLatitude(),
            entity.getLongitude(),
            entity.getIs24Hours(),
            entity.getTimezone(),
            entity.getWorkingHours(),
            entity.getAllowOnlineBooking(),
            entity.getDepositPercentage(),
            entity.getLogoUrl()
        );
    }
}