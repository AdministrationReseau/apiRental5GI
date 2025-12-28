package com.project.apirental.modules.staff.repository;

import com.project.apirental.modules.staff.domain.StaffEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface StaffRepository extends R2dbcRepository<StaffEntity, UUID> {

    /**
     * Utilisé pour vérifier si un utilisateur est déjà employé dans une organisation précise
     */
    Mono<StaffEntity> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * Récupérer tout le personnel d'une agence
     */
    Flux<StaffEntity> findAllByAgencyId(UUID agencyId);

    /**
     * Récupérer tout le personnel d'une organisation
     */
    Flux<StaffEntity> findAllByOrganizationId(UUID organizationId);
}