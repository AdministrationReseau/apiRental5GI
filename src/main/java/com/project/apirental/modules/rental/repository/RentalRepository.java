package com.project.apirental.modules.rental.repository;

import com.project.apirental.modules.rental.domain.RentalEntity;
import com.project.apirental.shared.enums.RentalStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RentalRepository extends R2dbcRepository<RentalEntity, UUID> {

    // --- REQUETES DE BASE ---
    Flux<RentalEntity> findAllByAgencyId(UUID agencyId);
    Flux<RentalEntity> findAllByClientId(UUID clientId);

    // --- FILTRES PAR LISTE DE STATUTS (Pour Client et Agence) ---

    // Pour le Client
    Flux<RentalEntity> findAllByClientIdAndStatusIn(UUID clientId, List<RentalStatus> statuses);

    // Pour l'Agence
    Flux<RentalEntity> findAllByAgencyIdAndStatusIn(UUID agencyId, List<RentalStatus> statuses);

    // --- REQUETES ORGANISATION (Jointure avec Agencies) ---

    // Récupérer les rentals d'une organisation filtrés par statuts
    @Query("""
        SELECT r.*
        FROM rentals r
        JOIN agencies a ON r.agency_id = a.id
        WHERE a.organization_id = :orgId
        AND r.status IN (:statuses)
        ORDER BY r.created_at DESC
    """)
    Flux<RentalEntity> findAllByOrganizationIdAndStatusIn(UUID orgId, List<RentalStatus> statuses);

    // --- UTILITAIRES ---

    // Pour vérifier les conflits de révision/maintenance
    @Query("SELECT COUNT(*) FROM rentals WHERE vehicle_id = :vehicleId AND start_date < :checkEnd AND end_date > :checkStart AND status NOT IN ('CANCELLED', 'COMPLETED')")
    Mono<Long> countConflictingRentals(UUID vehicleId, LocalDateTime checkStart, LocalDateTime checkEnd);
}
