package com.project.apirental.modules.notification.listener;

import com.project.apirental.modules.notification.services.NotificationService;
import com.project.apirental.shared.enums.NotificationResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

/**
 * Listener pour déclencher les notifications de manière asynchrone (Fire-and-Forget).
 *
 * Ce composant est utile si vous souhaitez découpler l'envoi de notification
 * du flux principal de transaction via ApplicationEventPublisher,
 * ou simplement pour centraliser les logs d'envoi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * Déclencher une notification de réservation
     */
    public void notifyReservation(
            UUID resourceId,
            NotificationResourceType resourceType,
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
        ).subscribeOn(Schedulers.boundedElastic()) // Exécution en arrière-plan
         .subscribe(
                 notification -> log.info("✉️ [Listener] Notification RESERVATION créée pour {} ({})", resourceType, resourceId),
                 error -> log.error("❌ [Listener] Erreur lors de la création de notification RESERVATION", error)
         );
    }

    /**
     * Déclencher une notification de début de location
     */
    public void notifyLocationStart(
            UUID resourceId,
            NotificationResourceType resourceType,
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
                 notification -> log.info("✉️ [Listener] Notification LOCATION_START créée pour {} ({})", resourceType, resourceId),
                 error -> log.error("❌ [Listener] Erreur lors de la création de notification LOCATION_START", error)
         );
    }

    /**
     * Déclencher une notification de fin de location
     */
    public void notifyLocationEnd(
            UUID resourceId,
            NotificationResourceType resourceType,
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
                 notification -> log.info("✉️ [Listener] Notification LOCATION_END créée pour {} ({})", resourceType, resourceId),
                 error -> log.error("❌ [Listener] Erreur lors de la création de notification LOCATION_END", error)
         );
    }
}
