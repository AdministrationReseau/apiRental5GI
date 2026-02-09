package com.project.apirental.modules.notification.repository;

import com.project.apirental.modules.notification.domain.NotificationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends R2dbcRepository<NotificationEntity, UUID> {

    // ... (Autres méthodes existantes : findAllByResourceId, findUnreadByResourceId, etc.) ...

    /**
     * Récupère les notifications pour une liste d'agences (pour l'agrégation par organisation)
     * Utilise la clause IN (:agencyIds)
     */
    @Query("SELECT * FROM notifications WHERE resource_id IN (:agencyIds) AND resource_type = 'AGENCY' ORDER BY created_at DESC")
    Flux<NotificationEntity> findNotificationsByAgencyIds(List<UUID> agencyIds);

    /**
     * Compte les notifications non lues pour une liste d'agences
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id IN (:agencyIds) AND resource_type = 'AGENCY' AND is_read = false")
    Mono<Long> countUnreadByAgencyIds(List<UUID> agencyIds);

    // ... (Méthodes existantes pour Agency, Driver, Client...)
    @Query("SELECT * FROM notifications WHERE resource_id = :agencyId AND resource_type = 'AGENCY' ORDER BY created_at DESC")
    Flux<NotificationEntity> findNotificationsByAgencyId(UUID agencyId);

    @Query("SELECT * FROM notifications WHERE resource_id = :agencyId AND resource_type = 'AGENCY' AND is_read = false ORDER BY created_at DESC")
    Flux<NotificationEntity> findUnreadNotificationsByAgencyId(UUID agencyId);

    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id = :agencyId AND resource_type = 'AGENCY' AND is_read = false")
    Mono<Long> countUnreadByAgencyId(UUID agencyId);

    // Méthodes génériques nécessaires au service
    Flux<NotificationEntity> findAllByResourceId(UUID resourceId);

    @Query("SELECT * FROM notifications WHERE resource_id = :resourceId AND is_read = false ORDER BY created_at DESC")
    Flux<NotificationEntity> findUnreadByResourceId(UUID resourceId);

    Mono<Long> countUnreadByResourceId(UUID resourceId);

    Flux<NotificationEntity> findAllByLocationId(UUID locationId);

    @Query("SELECT * FROM notifications WHERE resource_id = :resourceId AND resource_type = :resourceType AND reason = :reason ORDER BY created_at DESC")
    Flux<NotificationEntity> findByResourceIdAndTypeAndReason(UUID resourceId, String resourceType, String reason);

    // Méthodes Driver et Client (pour complétude du fichier)
    @Query("SELECT * FROM notifications WHERE resource_id = :driverId AND resource_type = 'DRIVER' ORDER BY created_at DESC")
    Flux<NotificationEntity> findNotificationsByDriverId(UUID driverId);

    @Query("SELECT * FROM notifications WHERE resource_id = :driverId AND resource_type = 'DRIVER' AND is_read = false ORDER BY created_at DESC")
    Flux<NotificationEntity> findUnreadNotificationsByDriverId(UUID driverId);

    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id = :driverId AND resource_type = 'DRIVER' AND is_read = false")
    Mono<Long> countUnreadByDriverId(UUID driverId);

    @Query("SELECT * FROM notifications WHERE resource_id = :clientId AND resource_type = 'CLIENT' ORDER BY created_at DESC")
    Flux<NotificationEntity> findNotificationsByClientId(UUID clientId);

    @Query("SELECT * FROM notifications WHERE resource_id = :clientId AND resource_type = 'CLIENT' AND is_read = false ORDER BY created_at DESC")
    Flux<NotificationEntity> findUnreadNotificationsByClientId(UUID clientId);

    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id = :clientId AND resource_type = 'CLIENT' AND is_read = false")
    Mono<Long> countUnreadByClientId(UUID clientId);
}
