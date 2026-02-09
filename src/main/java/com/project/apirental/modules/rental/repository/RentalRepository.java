package com.project.apirental.modules.rental.repository;

import com.project.apirental.modules.rental.domain.RentalEntity;
import com.project.apirental.shared.enums.RentalStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RentalRepository extends R2dbcRepository<RentalEntity, UUID> {
    Flux<RentalEntity> findAllByAgencyId(UUID agencyId);
    Flux<RentalEntity> findAllByClientId(UUID clientId);
    Flux<RentalEntity> findAllByAgencyIdAndStatus(UUID agencyId, RentalStatus status);

    // Pour vérifier les conflits de révision
    @Query("SELECT COUNT(*) FROM rentals WHERE vehicle_id = :vehicleId AND start_date < :checkEnd AND end_date > :checkStart AND status NOT IN ('CANCELLED', 'COMPLETED')")
    Mono<Long> countConflictingRentals(UUID vehicleId, LocalDateTime checkStart, LocalDateTime checkEnd);
}
