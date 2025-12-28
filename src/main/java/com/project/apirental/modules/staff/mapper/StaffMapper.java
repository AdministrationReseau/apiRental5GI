package com.project.apirental.modules.staff.mapper;

import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.poste.dto.PosteResponseDTO;
import com.project.apirental.modules.staff.domain.StaffEntity;
import com.project.apirental.modules.staff.dto.StaffResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class StaffMapper {

    /**
     * Transforme l'entité Staff et ses objets liés (déjà récupérés) en DTO de réponse.
     * 
     * @param entity L'entité Staff de la BDD
     * @param user L'entité User (du module Auth)
     * @param poste Le DTO du Poste (du module Poste)
     * @return StaffResponseDTO
     */
    public StaffResponseDTO toDto(StaffEntity entity, UserEntity user, PosteResponseDTO poste) {
        if (entity == null) return null;

        return new StaffResponseDTO(
            entity.getId(),
            user,
            entity.getAgencyId(),
            poste,
            entity.getStatus()
        );
    }
}