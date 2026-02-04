package com.project.apirental.modules.notification.listener;

import com.project.apirental.modules.notification.services.NotificationService;
import com.project.apirental.shared.enums.NotificationReason;
import com.project.apirental.shared.enums.NotificationResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

/**
 * Listener pour déclencher les notifications lors des événements de location.
 * 
 * À utiliser dans les services de location/réservation lors du :
 * - Créations de réservations
 * - Début de locations
 * - Fin de locations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * Déclencher une notification de réservation pour un client
     * 
     * Exemple d'utilisation :
     * applicationEventPublisher.publishEvent(
     *     new NotificationEvent("RESERVATION", resourceId, resourceType, locationId, vehicleId, driverId, details)
     * );
     */
    public void notifyReservation(
            UUID resourceId,
            String resourceType,
            UUID locationId,
            UUID vehicleId,
            UUID driverId,
            String details) {

        notificationService.createReservationNotification(
                resourceId,
                resourceType,
                locationId,
                vehicleId,
                driverId,
                details
        ).subscribeOn(Schedulers.boundedElastic())
         .subscribe(
                 notification -> log.info("✉️ Notification RESERVATION créée pour resource: {}", resourceId),
                 error -> log.error("❌ Erreur lors de la création de notification RESERVATION", error)
         );
    }

    /**
     * Déclencher une notification de début de location
     */
    public void notifyLocationStart(
            UUID resourceId,
            String resourceType,
            UUID locationId,
            UUID vehicleId,
            UUID driverId,
            String details) {

        notificationService.createLocationStartNotification(
                resourceId,
                resourceType,
                locationId,
                vehicleId,
                driverId,
                details
        ).subscribeOn(Schedulers.boundedElastic())
         .subscribe(
                 notification -> log.info("✉️ Notification LOCATION_START créée pour resource: {}", resourceId),
                 error -> log.error("❌ Erreur lors de la création de notification LOCATION_START", error)
         );
    }

    /**
     * Déclencher une notification de fin de location
     */
    public void notifyLocationEnd(
            UUID resourceId,
            String resourceType,
            UUID locationId,
            UUID vehicleId,
            UUID driverId,
            String details) {

        notificationService.createLocationEndNotification(
                resourceId,
                resourceType,
                locationId,
                vehicleId,
                driverId,
                details
        ).subscribeOn(Schedulers.boundedElastic())
         .subscribe(
                 notification -> log.info("✉️ Notification LOCATION_END créée pour resource: {}", resourceId),
                 error -> log.error("❌ Erreur lors de la création de notification LOCATION_END", error)
         );
    }
}
