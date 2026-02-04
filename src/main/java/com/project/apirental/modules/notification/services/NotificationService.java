package com.project.apirental.modules.notification.services;

import com.project.apirental.modules.notification.domain.NotificationEntity;
import com.project.apirental.modules.notification.dto.NotificationResponseDTO;
import com.project.apirental.modules.notification.mapper.NotificationMapper;
import com.project.apirental.modules.notification.repository.NotificationRepository;
import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.shared.enums.NotificationReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final AgencyRepository agencyRepository;

    /**
     * Créer une notification pour une réservation
     */
    @Transactional
    public Mono<NotificationResponseDTO> createReservationNotification(
            UUID resourceId,
            String resourceType,
            UUID locationId,
            UUID vehicleId,
            UUID driverId,
            String details) {
        return createNotification(
                locationId,
                resourceId,
                resourceType,
                NotificationReason.RESERVATION.name(),
                vehicleId,
                driverId,
                details
        );
    }

    /**
     * Créer une notification pour le début d'une location
     */
    @Transactional
    public Mono<NotificationResponseDTO> createLocationStartNotification(
            UUID resourceId,
            String resourceType,
            UUID locationId,
            UUID vehicleId,
            UUID driverId,
            String details) {
        return createNotification(
                locationId,
                resourceId,
                resourceType,
                NotificationReason.LOCATION_START.name(),
                vehicleId,
                driverId,
                details
        );
    }

    /**
     * Créer une notification pour la fin d'une location
     */
    @Transactional
    public Mono<NotificationResponseDTO> createLocationEndNotification(
            UUID resourceId,
            String resourceType,
            UUID locationId,
            UUID vehicleId,
            UUID driverId,
            String details) {
        return createNotification(
                locationId,
                resourceId,
                resourceType,
                NotificationReason.LOCATION_END.name(),
                vehicleId,
                driverId,
                details
        );
    }

    /**
     * Créer une notification générique
     */
    @Transactional
    public Mono<NotificationResponseDTO> createNotification(
            UUID locationId,
            UUID resourceId,
            String resourceType,
            String reason,
            UUID vehicleId,
            UUID driverId,
            String details) {

        NotificationEntity notification = NotificationEntity.builder()
                .id(UUID.randomUUID())
                .locationId(locationId)
                .resourceId(resourceId)
                .resourceType(resourceType)
                .reason(reason)
                .vehicleId(vehicleId)
                .driverId(driverId)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .details(details)
                .isNewRecord(true)
                .build();

        return notificationRepository.save(Objects.requireNonNull(notification))
                .map(notificationMapper::toDto);
    }

    /**
     * Récupérer toutes les notifications pour une ressource
     */
    public Flux<NotificationResponseDTO> getNotificationsByResourceId(UUID resourceId) {
        return notificationRepository.findAllByResourceId(resourceId)
                .map(notificationMapper::toDto);
    }

    /**
     * Récupérer les notifications non lues pour une ressource
     */
    public Flux<NotificationResponseDTO> getUnreadNotifications(UUID resourceId) {
        return notificationRepository.findUnreadByResourceId(resourceId)
                .map(notificationMapper::toDto);
    }

    /**
     * Compter les notifications non lues pour une ressource
     */
    public Mono<Long> countUnreadNotifications(UUID resourceId) {
        return notificationRepository.countUnreadByResourceId(resourceId);
    }

    /**
     * Marquer une notification comme lue
     */
    @Transactional
    public Mono<NotificationResponseDTO> markAsRead(UUID notificationId) {
        return notificationRepository.findById(Objects.requireNonNull(notificationId))
                .flatMap(notification -> {
                    notification.setIsRead(true);
                    return notificationRepository.save(notification);
                })
                .map(notificationMapper::toDto);
    }

    /**
     * Marquer toutes les notifications comme lues pour une ressource
     */
    @Transactional
    public Mono<Void> markAllAsRead(UUID resourceId) {
        return notificationRepository.findAllByResourceId(Objects.requireNonNull(resourceId))
                .flatMap(notification -> {
                    notification.setIsRead(true);
                    return notificationRepository.save(notification);
                })
                .then();
    }

    /**
     * Récupérer les notifications d'une location
     */
    public Flux<NotificationResponseDTO> getNotificationsByLocation(UUID locationId) {
        return notificationRepository.findAllByLocationId(Objects.requireNonNull(locationId))
                .map(notificationMapper::toDto);
    }

    /**
     * Supprimer une notification
     */
    @Transactional
    public Mono<Void> deleteNotification(UUID notificationId) {
        return notificationRepository.deleteById(Objects.requireNonNull(notificationId));
    }

    /**
     * Récupérer les notifications d'une ressource par type et raison
     */
    public Flux<NotificationResponseDTO> getNotificationsByResourceAndTypeAndReason(
            UUID resourceId,
            String resourceType,
            String reason) {
        return notificationRepository.findByResourceIdAndTypeAndReason(
                Objects.requireNonNull(resourceId),
                resourceType,
                reason
        ).map(notificationMapper::toDto);
    }

    /**
     * Récupérer les notifications d'une agence
     */
    public Flux<NotificationResponseDTO> getNotificationsByAgencyId(UUID agencyId) {
        return notificationRepository.findNotificationsByAgencyId(Objects.requireNonNull(agencyId))
                .map(notificationMapper::toDto);
    }

    /**
     * Récupérer les notifications non lues d'une agence
     */
    public Flux<NotificationResponseDTO> getUnreadNotificationsByAgencyId(UUID agencyId) {
        return notificationRepository.findUnreadNotificationsByAgencyId(Objects.requireNonNull(agencyId))
                .map(notificationMapper::toDto);
    }

    /**
     * Compter les notifications non lues d'une agence
     */
    public Mono<Long> countUnreadByAgencyId(UUID agencyId) {
        return notificationRepository.countUnreadByAgencyId(Objects.requireNonNull(agencyId));
    }

    /**
     * Récupérer les notifications d'un chauffeur
     */
    public Flux<NotificationResponseDTO> getNotificationsByDriverId(UUID driverId) {
        return notificationRepository.findNotificationsByDriverId(Objects.requireNonNull(driverId))
                .map(notificationMapper::toDto);
    }

    /**
     * Récupérer les notifications non lues d'un chauffeur
     */
    public Flux<NotificationResponseDTO> getUnreadNotificationsByDriverId(UUID driverId) {
        return notificationRepository.findUnreadNotificationsByDriverId(Objects.requireNonNull(driverId))
                .map(notificationMapper::toDto);
    }

    /**
     * Compter les notifications non lues d'un chauffeur
     */
    public Mono<Long> countUnreadByDriverId(UUID driverId) {
        return notificationRepository.countUnreadByDriverId(Objects.requireNonNull(driverId));
    }

    /**
     * Récupérer les notifications d'un client
     */
    public Flux<NotificationResponseDTO> getNotificationsByClientId(UUID clientId) {
        return notificationRepository.findNotificationsByClientId(Objects.requireNonNull(clientId))
                .map(notificationMapper::toDto);
    }

    /**
     * Récupérer les notifications non lues d'un client
     */
    public Flux<NotificationResponseDTO> getUnreadNotificationsByClientId(UUID clientId) {
        return notificationRepository.findUnreadNotificationsByClientId(Objects.requireNonNull(clientId))
                .map(notificationMapper::toDto);
    }

    /**
     * Compter les notifications non lues d'un client
     */
    public Mono<Long> countUnreadByClientId(UUID clientId) {
        return notificationRepository.countUnreadByClientId(Objects.requireNonNull(clientId));
    }

    /**
     * Récupérer toutes les notifications d'une organisation (agrégat des notifications de ses agences)
     */
    public Flux<NotificationResponseDTO> getNotificationsByOrganizationId(UUID organizationId) {
        return agencyRepository.findAllByOrganizationId(Objects.requireNonNull(organizationId))
                .map(agency -> agency.getId())
                .collectList()
                .flatMapMany(agencyIds -> {
                    if (agencyIds.isEmpty()) {
                        return Flux.empty();
                    }
                    return notificationRepository.findNotificationsByAgencyIds(agencyIds)
                            .map(notificationMapper::toDto);
                });
    }

    /**
     * Récupérer les notifications non lues d'une organisation (agrégat de ses agences)
     */
    public Flux<NotificationResponseDTO> getUnreadNotificationsByOrganizationId(UUID organizationId) {
        return agencyRepository.findAllByOrganizationId(Objects.requireNonNull(organizationId))
                .map(agency -> agency.getId())
                .collectList()
                .flatMapMany(agencyIds -> {
                    if (agencyIds.isEmpty()) {
                        return Flux.empty();
                    }
                    return notificationRepository.findNotificationsByAgencyIds(agencyIds)
                            .filter(n -> !n.getIsRead())
                            .map(notificationMapper::toDto);
                });
    }

    /**
     * Compter les notifications non lues d'une organisation (agrégat de ses agences)
     */
    public Mono<Long> countUnreadByOrganizationId(UUID organizationId) {
        return agencyRepository.findAllByOrganizationId(Objects.requireNonNull(organizationId))
                .map(agency -> agency.getId())
                .collectList()
                .flatMap(agencyIds -> {
                    if (agencyIds.isEmpty()) {
                        return Mono.just(0L);
                    }
                    return notificationRepository.countUnreadByAgencyIds(agencyIds);
                });
    }
}
