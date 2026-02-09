package com.project.apirental.modules.notification.api;

import com.project.apirental.modules.notification.dto.NotificationCreateRequest;
import com.project.apirental.modules.notification.dto.NotificationResponseDTO;
import com.project.apirental.modules.notification.services.NotificationService;
import com.project.apirental.shared.enums.NotificationReason;
import com.project.apirental.shared.enums.NotificationResourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Gestion des notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Créer une notification (Interne/Test)")
    @PostMapping
    public Mono<ResponseEntity<NotificationResponseDTO>> createNotification(
            @RequestBody NotificationCreateRequest request) {
        return notificationService.createNotification(
                request.locationId(),
                request.resourceId(),
                NotificationResourceType.valueOf(request.resourceType()),
                NotificationReason.valueOf(request.reason()),
                request.vehicleId(),
                request.driverId(),
                request.details()
        ).map(ResponseEntity::ok);
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PutMapping("/{notificationId}/read")
    public Mono<ResponseEntity<NotificationResponseDTO>> markAsRead(@PathVariable UUID notificationId) {
        return notificationService.markAsRead(notificationId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer une notification")
    @DeleteMapping("/{notificationId}")
    public Mono<ResponseEntity<Void>> deleteNotification(@PathVariable UUID notificationId) {
        return notificationService.deleteNotification(notificationId)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @Operation(summary = "Récupérer les notifications d'une location")
    @GetMapping("/location/{locationId}")
    public Flux<NotificationResponseDTO> getNotificationsByLocation(@PathVariable UUID locationId) {
        return notificationService.getNotificationsByLocation(locationId);
    }

    @Operation(summary = "Filtrer les notifications")
    @GetMapping("/resource/{resourceId}/filter")
    public Flux<NotificationResponseDTO> getNotificationsByFilter(
            @PathVariable UUID resourceId,
            @RequestParam String resourceType,
            @RequestParam String reason) {
        return notificationService.getNotificationsByResourceAndTypeAndReason(
                resourceId,
                resourceType,
                reason
        );
    }

    // ========== ENDPOINTS ORGANISATION (AGRÉGAT) ==========

    @Operation(summary = "Récupérer les notifications d'une organisation (toutes ses agences)")
    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<NotificationResponseDTO> getNotificationsByOrganization(@PathVariable UUID organizationId) {
        return notificationService.getNotificationsByOrganizationId(organizationId);
    }

    @Operation(summary = "Récupérer les notifications non lues d'une organisation")
    @GetMapping("/organization/{organizationId}/unread")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<NotificationResponseDTO> getUnreadNotificationsByOrganization(@PathVariable UUID organizationId) {
        return notificationService.getUnreadNotificationsByOrganizationId(organizationId);
    }

    @Operation(summary = "Compter les notifications non lues d'une organisation")
    @GetMapping("/organization/{organizationId}/unread/count")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<Long> countUnreadByOrganization(@PathVariable UUID organizationId) {
        return notificationService.countUnreadByOrganizationId(organizationId);
    }

    // ========== ENDPOINTS AGENCE ==========

    @Operation(summary = "Récupérer les notifications d'une agence")
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Flux<NotificationResponseDTO> getNotificationsByAgency(@PathVariable UUID agencyId) {
        return notificationService.getNotificationsByAgencyId(agencyId);
    }

    @Operation(summary = "Récupérer les notifications non lues d'une agence")
    @GetMapping("/agency/{agencyId}/unread")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Flux<NotificationResponseDTO> getUnreadNotificationsByAgency(@PathVariable UUID agencyId) {
        return notificationService.getUnreadNotificationsByAgencyId(agencyId);
    }

    @Operation(summary = "Compter les notifications non lues d'une agence")
    @GetMapping("/agency/{agencyId}/unread/count")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Mono<Long> countUnreadByAgency(@PathVariable UUID agencyId) {
        return notificationService.countUnreadByAgencyId(agencyId);
    }

    // ========== ENDPOINTS CHAUFFEUR ==========

    @Operation(summary = "Récupérer les notifications d'un chauffeur")
    @GetMapping("/driver/{driverId}")
    public Flux<NotificationResponseDTO> getNotificationsByDriver(@PathVariable UUID driverId) {
        return notificationService.getNotificationsByDriverId(driverId);
    }

    @Operation(summary = "Compter les notifications non lues d'un chauffeur")
    @GetMapping("/driver/{driverId}/unread/count")
    public Mono<Long> countUnreadByDriver(@PathVariable UUID driverId) {
        return notificationService.countUnreadByDriverId(driverId);
    }

    // ========== ENDPOINTS CLIENT ==========

    @Operation(summary = "Récupérer les notifications d'un client")
    @GetMapping("/client/{clientId}")
    public Flux<NotificationResponseDTO> getNotificationsByClient(@PathVariable UUID clientId) {
        return notificationService.getNotificationsByClientId(clientId);
    }

    @Operation(summary = "Compter les notifications non lues d'un client")
    @GetMapping("/client/{clientId}/unread/count")
    public Mono<Long> countUnreadByClient(@PathVariable UUID clientId) {
        return notificationService.countUnreadByClientId(clientId);
    }
}
