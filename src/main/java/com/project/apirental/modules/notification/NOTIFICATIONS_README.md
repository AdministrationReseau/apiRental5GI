# Service de Notifications - Guide d'utilisation

## Vue d'ensemble

Le service de notifications permet de déclencher des alertes pour les ressources (clients, chauffeurs, agences) lors d'événements importants dans le cycle de location :
- **RESERVATION** : Lors de la création d'une réservation
- **LOCATION_START** : Au début d'une location
- **LOCATION_END** : À la fin d'une location

## Structure de données

### NotificationEntity
La table `notifications` contient les champs suivants :

| Champ | Type | Description |
|-------|------|-------------|
| `id` | UUID | Identifiant unique (PK) |
| `location_id` | UUID | Référence à la location associée |
| `resource_id` | UUID | ID de la ressource destinataire |
| `resource_type` | VARCHAR(50) | Type de ressource : CLIENT, DRIVER, AGENCY |
| `reason` | VARCHAR(50) | Motif : RESERVATION, LOCATION_START, LOCATION_END |
| `vehicle_id` | UUID | Véhicule impliqué (optionnel) |
| `driver_id` | UUID | Chauffeur impliqué (optionnel) |
| `created_at` | TIMESTAMP | Date/heure de création |
| `is_read` | BOOLEAN | Statut de lecture |
| `details` | TEXT | Détails additionnels (JSON, message, etc.) |

### Index de performance
- `idx_notifications_resource_id` : Recherche par ressource
- `idx_notifications_location_id` : Recherche par location
- `idx_notifications_resource_id_unread` : Recherche rapide des notifications non lues
- `idx_notifications_created_at` : Tri par date

## API REST

### Endpoints disponibles

#### 1. Créer une notification
```http
POST /api/notifications
Content-Type: application/json

{
  "locationId": "uuid-location",
  "resourceId": "uuid-client-ou-chauffeur",
  "resourceType": "CLIENT",
  "reason": "RESERVATION",
  "vehicleId": "uuid-vehicle",
  "driverId": "uuid-driver",
  "details": "Votre réservation a été confirmée"
}
```

#### 2. Récupérer les notifications d'une ressource
```http
GET /api/notifications/resource/{resourceId}
```

#### 3. Récupérer les notifications non lues
```http
GET /api/notifications/resource/{resourceId}/unread
```

#### 4. Compter les notifications non lues
```http
GET /api/notifications/resource/{resourceId}/unread/count
```

Réponse :
```
200 (nombre de notifications non lues)
```

#### 5. Marquer comme lue
```http
PUT /api/notifications/{notificationId}/read
```

#### 6. Marquer toutes comme lues
```http
PUT /api/notifications/resource/{resourceId}/read-all
```

#### 7. Récupérer les notifications d'une location
```http
GET /api/notifications/location/{locationId}
```

#### 8. Supprimer une notification
```http
DELETE /api/notifications/{notificationId}
```

#### 9. Filtrer par type et raison
```http
GET /api/notifications/resource/{resourceId}/filter?resourceType=CLIENT&reason=RESERVATION
```

## Intégration dans vos services

### Exemple 1 : Déclencher une notification lors d'une réservation

Dans votre service de location/réservation :

```java
@Service
@RequiredArgsConstructor
public class LocationService {
    
    private final NotificationService notificationService;
    
    @Transactional
    public Mono<LocationResponseDTO> createReservation(LocationCreateRequest request) {
        // ... logique de création de location ...
        
        return locationRepository.save(location)
                .flatMap(savedLocation -> {
                    // Créer une notification pour le client
                    return notificationService.createReservationNotification(
                            clientId,                    // resourceId
                            "CLIENT",                    // resourceType
                            savedLocation.getId(),       // locationId
                            request.vehicleId(),         // vehicleId
                            request.driverId(),          // driverId (peut être null)
                            "Votre réservation a été confirmée"  // details
                    ).thenReturn(savedLocation);
                })
                .map(locationMapper::toDto);
    }
}
```

### Exemple 2 : Utiliser le listener d'événements

```java
@Component
@RequiredArgsConstructor
public class LocationEventHandler {
    
    private final NotificationEventListener notificationListener;
    
    public void onLocationStarted(LocationStartedEvent event) {
        notificationListener.notifyLocationStart(
                event.getResourceId(),
                "CLIENT",
                event.getLocationId(),
                event.getVehicleId(),
                event.getDriverId(),
                "Votre location a commencé"
        );
    }
}
```

### Exemple 3 : Déclencher une notification pour multiple ressources

```java
@Transactional
public Mono<Void> notifyAllStakeholders(UUID locationId, UUID vehicleId, UUID driverId) {
    return Mono.when(
        // Notifier le client
        notificationService.createLocationStartNotification(
            clientId, "CLIENT", locationId, vehicleId, driverId, 
            "Votre location a commencé"
        ),
        // Notifier l'agence
        notificationService.createLocationStartNotification(
            agencyId, "AGENCY", locationId, vehicleId, driverId,
            "Une location a commencé dans votre agence"
        ),
        // Notifier le chauffeur (si applicable)
        driverId != null ? notificationService.createLocationStartNotification(
            driverId, "DRIVER", locationId, vehicleId, driverId,
            "Vous avez été assigné à une location"
        ) : Mono.empty()
    );
}
```

## Patterns de notifications recommandés

### Pattern 1 : Notification systématique
Chaque événement important déclenche automatiquement une notification.

```java
// Dans LocationService.createLocation()
Mono<LocationResponseDTO> result = locationRepository.save(location)
    .flatMap(savedLocation -> 
        notificationService.createReservationNotification(...)
            .thenReturn(savedLocation)
    )
    .map(locationMapper::toDto);
```

### Pattern 2 : Notification avec filtre
Notifier uniquement certaines ressources selon des critères.

```java
if (location.requiresApproval()) {
    notificationService.createNotification(
        locationId, agencyId, "AGENCY", "RESERVATION",
        vehicleId, driverId,
        "Nouvelle réservation en attente d'approbation"
    );
}
```

### Pattern 3 : Notification avec détails JSON
Inclure des informations structurées dans le champ `details`.

```java
String details = objectMapper.writeValueAsString(
    new NotificationDetails(
        amount: location.getTotalPrice(),
        vehicleInfo: vehicle.toString(),
        dates: location.getRentalPeriod()
    )
);

notificationService.createNotification(
    locationId, resourceId, resourceType, reason,
    vehicleId, driverId, details
);
```

## Considérations de performance

1. **Index** : Les 4 index sur `notifications` sont optimisés pour les requêtes courantes.
2. **Statut de lecture** : Index combiné `resource_id + is_read` pour les notifications non lues.
3. **Archive** : Envisager une archivage des anciennes notifications (> 6 mois) pour les tables volumineuses.
4. **Async** : Utiliser `subscribeOn(Schedulers.boundedElastic())` pour les créations non-critiques.

## Sécurité

- Les endpoints `/api/notifications` nécessitent l'authentification JWT
- Vérifier les permissions : une ressource ne peut voir que ses propres notifications
- Ajouter une validation dans le contrôleur :

```java
@GetMapping("/resource/{resourceId}")
public Flux<NotificationResponseDTO> getNotifications(@PathVariable UUID resourceId) {
    // Vérifier que l'utilisateur accède à ses propres notifications
    return securityService.getCurrentResourceId()
        .filter(currentId -> currentId.equals(resourceId))
        .flux()
        .flatMap(id -> notificationService.getNotificationsByResourceId(resourceId))
        .switchIfEmpty(error(new UnauthorizedException()));
}
```

## Migrations futures

1. **WebSocket** : Notifications en temps réel pour les clients connectés.
2. **Email/SMS** : Intégration d'un service d'envoi de messages.
3. **Batch** : Notifications groupées quotidiennes/hebdomadaires.
4. **Templates** : Système de templates pour les messages.
5. **Préférences** : Permettre aux utilisateurs de configurer les types de notifications qu'ils reçoivent.
