package com.project.apirental.modules.notification.services;

import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.notification.domain.NotificationEntity;
import com.project.apirental.modules.notification.domain.NotificationTemplate;
import com.project.apirental.modules.notification.dto.NotificationResponseDTO;
import com.project.apirental.modules.notification.mapper.NotificationMapper;
import com.project.apirental.modules.notification.repository.NotificationRepository;
import com.project.apirental.shared.enums.NotificationReason;
import com.project.apirental.shared.enums.NotificationResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final AgencyRepository agencyRepository;

    /**
     * Création de notification via TEMPLATE (Méthode recommandée)
     */
    @Transactional
    public Mono<NotificationResponseDTO> createNotification(
            UUID locationId,
            UUID resourceId,
            NotificationResourceType resourceType,
            NotificationReason reason,
            UUID vehicleId,
            UUID driverId,
            NotificationTemplate template,
            Object... args) {

        // Formatage du message à partir du template
        String details = template.format(args);

        return createNotificationRaw(locationId, resourceId, resourceType, reason, vehicleId, driverId, details);
    }

    /**
     * Création de notification avec message brut (Méthode bas niveau)
     */
    @Transactional
    public Mono<NotificationResponseDTO> createNotificationRaw(
            UUID locationId,
            UUID resourceId,
            NotificationResourceType resourceType,
            NotificationReason reason,
            UUID vehicleId,
            UUID driverId,
            String details) {

        NotificationEntity notification = NotificationEntity.builder()
                .id(UUID.randomUUID())
                .locationId(locationId)
                .resourceId(resourceId)
                .resourceType(resourceType.name())
                .reason(reason.name())
                .vehicleId(vehicleId)
                .driverId(driverId)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .details(details)
                .isNewRecord(true)
                .build();

        return notificationRepository.save(notification)
                .map(notificationMapper::toDto);
    }

    // --- LECTURE (CLIENT) ---
    public Flux<NotificationResponseDTO> getClientNotifications(UUID clientId) {
        return notificationRepository.findNotificationsByClientId(clientId).map(notificationMapper::toDto);
    }
    public Mono<Long> countUnreadClient(UUID clientId) {
        return notificationRepository.countUnreadByClientId(clientId);
    }

    // --- LECTURE (AGENCE) ---
    public Flux<NotificationResponseDTO> getAgencyNotifications(UUID agencyId) {
        return notificationRepository.findNotificationsByAgencyId(agencyId).map(notificationMapper::toDto);
    }
    public Mono<Long> countUnreadAgency(UUID agencyId) {
        return notificationRepository.countUnreadByAgencyId(agencyId);
    }

    // --- LECTURE (ORGANISATION - Agrégation) ---
    public Flux<NotificationResponseDTO> getOrganizationNotifications(UUID orgId) {
        return agencyRepository.findAllByOrganizationId(orgId)
                .map(agency -> agency.getId())
                .collectList()
                .flatMapMany(ids -> {
                    if (ids.isEmpty()) return Flux.empty();
                    return notificationRepository.findNotificationsByAgencyIds(ids);
                })
                .map(notificationMapper::toDto);
    }

    public Mono<Long> countUnreadOrganization(UUID orgId) {
        return agencyRepository.findAllByOrganizationId(orgId)
                .map(agency -> agency.getId())
                .collectList()
                .flatMap(ids -> {
                    if (ids.isEmpty()) return Mono.just(0L);
                    return notificationRepository.countUnreadByAgencyIds(ids);
                });
    }

    // --- ACTIONS ---
    @Transactional
    public Mono<Void> markAsRead(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .flatMap(n -> {
                    n.setIsRead(true);
                    return notificationRepository.save(n);
                }).then();
    }

    @Transactional
    public Mono<Void> deleteNotification(UUID notificationId) {
        return notificationRepository.deleteById(notificationId);
    }
}
