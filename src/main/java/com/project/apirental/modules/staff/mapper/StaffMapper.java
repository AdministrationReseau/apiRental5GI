package com.project.apirental.modules.staff.mapper;

import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.poste.dto.PosteResponseDTO;
import com.project.apirental.modules.staff.dto.StaffResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class StaffMapper {

    public StaffResponseDTO toDto(UserEntity user, PosteResponseDTO poste) {
        if (user == null) return null;

        return new StaffResponseDTO(
            user.getId(),
            user.getFirstname(),
            user.getLastname(),
            user.getEmail(),
            user.getAgencyId(),
            poste,
            user.getStatus(),
            user.getHiredAt()
        );
    }
}