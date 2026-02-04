package com.project.apirental.modules.notification.repository;

import com.project.apirental.modules.notification.domain.NotificationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface NotificationRepository extends R2dbcRepository<NotificationEntity, UUID> {

    /**
     * Récupère toutes les notifications pour une ressource (client, chauffeur ou agence)
     */
    Flux<NotificationEntity> findAllByResourceId(UUID resourceId);

    /**
     * Récupère toutes les notifications non lues pour une ressource
     */
    @Query("SELECT * FROM notifications WHERE resource_id = :resourceId AND is_read = false ORDER BY created_at DESC")
    Flux<NotificationEntity> findUnreadByResourceId(UUID resourceId);

    /**
     * Récupère les notifications d'une location
     */
    Flux<NotificationEntity> findAllByLocationId(UUID locationId);

    /**
     * Récupère les notifications d'une ressource par type et raison
     */
    @Query("SELECT * FROM notifications WHERE resource_id = :resourceId AND resource_type = :resourceType AND reason = :reason ORDER BY created_at DESC")
    Flux<NotificationEntity> findByResourceIdAndTypeAndReason(UUID resourceId, String resourceType, String reason);

    /**
     * Compte les notifications non lues pour une ressource
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id = :resourceId AND is_read = false")
    Mono<Long> countUnreadByResourceId(UUID resourceId);

    /**
     * Récupère la notification la plus récente pour une location et une ressource
     */
    @Query("SELECT * FROM notifications WHERE location_id = :locationId AND resource_id = :resourceId ORDER BY created_at DESC LIMIT 1")
    Mono<NotificationEntity> findLatestByLocationAndResource(UUID locationId, UUID resourceId);

    /**
     * Récupère les notifications d'une agence (resource_type = AGENCY)
     */
    @Query("SELECT * FROM notifications WHERE resource_id = :agencyId AND resource_type = 'AGENCY' ORDER BY created_at DESC")
    Flux<NotificationEntity> findNotificationsByAgencyId(UUID agencyId);

    /**
     * Récupère les notifications non lues d'une agence
     */
    @Query("SELECT * FROM notifications WHERE resource_id = :agencyId AND resource_type = 'AGENCY' AND is_read = false ORDER BY created_at DESC")
    Flux<NotificationEntity> findUnreadNotificationsByAgencyId(UUID agencyId);

    /**
     * Récupère les notifications d'un chauffeur (resource_type = DRIVER)
     */
    @Query("SELECT * FROM notifications WHERE resource_id = :driverId AND resource_type = 'DRIVER' ORDER BY created_at DESC")
    Flux<NotificationEntity> findNotificationsByDriverId(UUID driverId);

    /**
     * Récupère les notifications non lues d'un chauffeur
     */
    @Query("SELECT * FROM notifications WHERE resource_id = :driverId AND resource_type = 'DRIVER' AND is_read = false ORDER BY created_at DESC")
    Flux<NotificationEntity> findUnreadNotificationsByDriverId(UUID driverId);

    /**
     * Récupère les notifications d'un client (resource_type = CLIENT)
     */
    @Query("SELECT * FROM notifications WHERE resource_id = :clientId AND resource_type = 'CLIENT' ORDER BY created_at DESC")
    Flux<NotificationEntity> findNotificationsByClientId(UUID clientId);

    /**
     * Récupère les notifications non lues d'un client
     */
    @Query("SELECT * FROM notifications WHERE resource_id = :clientId AND resource_type = 'CLIENT' AND is_read = false ORDER BY created_at DESC")
    Flux<NotificationEntity> findUnreadNotificationsByClientId(UUID clientId);

    /**
     * Compte les notifications non lues d'une agence
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id = :agencyId AND resource_type = 'AGENCY' AND is_read = false")
    Mono<Long> countUnreadByAgencyId(UUID agencyId);

    /**
     * Compte les notifications non lues d'un chauffeur
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id = :driverId AND resource_type = 'DRIVER' AND is_read = false")
    Mono<Long> countUnreadByDriverId(UUID driverId);

    /**
     * Compte les notifications non lues d'un client
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id = :clientId AND resource_type = 'CLIENT' AND is_read = false")
    Mono<Long> countUnreadByClientId(UUID clientId);

    /**
     * Récupère les notifications pour une liste d'agences (pour l'agrégation par organisation)
     */
    @Query("SELECT * FROM notifications WHERE resource_id IN (:agencyIds) AND resource_type = 'AGENCY' ORDER BY created_at DESC")
    Flux<NotificationEntity> findNotificationsByAgencyIds(java.util.List<UUID> agencyIds);

    /**
     * Compte les notifications non lues pour une liste d'agences
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id IN (:agencyIds) AND resource_type = 'AGENCY' AND is_read = false")
    Mono<Long> countUnreadByAgencyIds(java.util.List<UUID> agencyIds);
}

