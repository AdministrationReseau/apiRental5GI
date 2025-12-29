package com.project.apirental.modules.driver.mapper;

import com.project.apirental.modules.driver.domain.DriverEntity;
import com.project.apirental.modules.driver.dto.DriverResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {
    public DriverResponseDTO toDto(DriverEntity entity) {
        if (entity == null) return null;
        return new DriverResponseDTO(
            entity.getId(),
            entity.getOrganizationId(),
            entity.getAgencyId(),
            entity.getFirstname(),
            entity.getLastname(),
            entity.getTel(),
            entity.getAge(),
            entity.getGender(),
            entity.getProfilUrl(),
            entity.getCniUrl(),
            entity.getDrivingLicenseUrl(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
