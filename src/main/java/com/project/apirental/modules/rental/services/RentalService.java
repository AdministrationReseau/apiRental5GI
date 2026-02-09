package com.project.apirental.modules.rental.services;

import com.project.apirental.modules.agency.mapper.AgencyMapper;
import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.notification.services.NotificationService;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.pricing.services.PricingService;
import com.project.apirental.modules.rental.domain.RentalEntity;
import com.project.apirental.modules.rental.dto.AgencyRentalRequest;
import com.project.apirental.modules.rental.dto.RentalInitRequest;
import com.project.apirental.modules.rental.dto.RentalInitResponse;
import com.project.apirental.modules.rental.repository.RentalRepository;
import com.project.apirental.modules.schedule.services.ScheduleService;
import com.project.apirental.modules.vehicle.repository.VehicleRepository;
import com.project.apirental.shared.dto.ScheduleRequestDTO;
import com.project.apirental.shared.enums.NotificationReason;
import com.project.apirental.shared.enums.NotificationResourceType;
import com.project.apirental.shared.enums.RentalStatus;
import com.project.apirental.shared.enums.RentalType;
import com.project.apirental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final VehicleRepository vehicleRepository;
    private final AgencyRepository agencyRepository;
    private final OrganizationRepository organizationRepository;
    private final PricingService pricingService;
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;
    private final AgencyMapper agencyMapper;

    /**
     * Étape 1 : Initialiser une location (Client via App)
     */
    @Transactional
    public Mono<RentalInitResponse> initiateRental(UUID clientId, RentalInitRequest request) {
        return vehicleRepository.findById(request.vehicleId())
            .switchIfEmpty(Mono.error(new RuntimeException("Véhicule non trouvé")))
            .flatMap(vehicle -> organizationRepository.findById(vehicle.getOrganizationId())
                .flatMap(org -> {
                    if (Boolean.FALSE.equals(org.getIsDriverBookingRequired())) {
                        return agencyRepository.findById(vehicle.getAgencyId())
                            .map(agency -> new RentalInitResponse(
                                false,
                                "La location sans chauffeur nécessite de contacter l'agence directement.",
                                null, null, null, null,
                                agencyMapper.toDto(agency)
                            ));
                    }

                    return Mono.zip(
                        pricingService.getPricing(ResourceType.VEHICLE, request.vehicleId()),
                        pricingService.getPricing(ResourceType.DRIVER, request.driverId()),
                        agencyRepository.findById(vehicle.getAgencyId())
                    ).flatMap(tuple -> {
                        var vehiclePrice = tuple.getT1();
                        var driverPrice = tuple.getT2();
                        var agency = tuple.getT3();

                        long duration = (request.rentalType() == RentalType.DAILY)
                            ? Math.max(1, Duration.between(request.startDate(), request.endDate()).toDays())
                            : Math.max(1, Duration.between(request.startDate(), request.endDate()).toHours());

                        BigDecimal vPrice = (request.rentalType() == RentalType.DAILY) ? vehiclePrice.getPricePerDay() : vehiclePrice.getPricePerHour();
                        BigDecimal dPrice = (request.rentalType() == RentalType.DAILY) ? driverPrice.getPricePerDay() : driverPrice.getPricePerHour();

                        BigDecimal subTotal = vPrice.add(dPrice).multiply(BigDecimal.valueOf(duration));
                        BigDecimal commission = subTotal.multiply(BigDecimal.valueOf(0.20));
                        BigDecimal deposit = subTotal.multiply(BigDecimal.valueOf(agency.getDepositPercentage() / 100.0));
                        BigDecimal total = subTotal.add(commission).add(deposit);

                        RentalEntity rental = RentalEntity.builder()
                            .id(UUID.randomUUID())
                            .clientId(clientId)
                            .agencyId(vehicle.getAgencyId())
                            .vehicleId(request.vehicleId())
                            .driverId(request.driverId())
                            .startDate(request.startDate())
                            .endDate(request.endDate())
                            .status(RentalStatus.PENDING)
                            .rentalType(request.rentalType())
                            .totalAmount(total)
                            .amountPaid(BigDecimal.ZERO)
                            .commissionAmount(commission)
                            .depositAmount(deposit)
                            .clientPhone(request.clientPhone())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .isNewRecord(true)
                            .build();

                        return rentalRepository.save(rental)
                            .flatMap(saved ->
                                // Notification : Client + Agence (Pas encore chauffeur car non payé)
                                notifyStakeholders(saved, NotificationReason.RESERVATION,
                                    "Votre demande de réservation a été créée. Veuillez procéder au paiement.",
                                    "Nouvelle demande de réservation reçue (Client App).",
                                    null // Pas de notif chauffeur à ce stade
                                ).thenReturn(new RentalInitResponse(
                                    true, "Location initiée. En attente de paiement.",
                                    saved.getId(), total, deposit, commission, agencyMapper.toDto(agency)
                                ))
                            );
                    });
                }));
    }

    /**
     * Création d'une location directement par l'agence (Walk-in)
     */
    @Transactional
    public Mono<RentalInitResponse> createAgencyRental(UUID agencyId, AgencyRentalRequest request) {
        return vehicleRepository.findById(request.vehicleId())
            .filter(v -> v.getAgencyId().equals(agencyId))
            .switchIfEmpty(Mono.error(new RuntimeException("Véhicule non trouvé ou n'appartient pas à cette agence")))
            .flatMap(vehicle -> {
                return Mono.zip(
                    pricingService.getPricing(ResourceType.VEHICLE, request.vehicleId()),
                    pricingService.getPricing(ResourceType.DRIVER, request.driverId())
                ).flatMap(tuple -> {
                    var vehiclePrice = tuple.getT1();
                    var driverPrice = tuple.getT2();

                    long duration = (request.rentalType() == RentalType.DAILY)
                        ? Math.max(1, Duration.between(request.startDate(), request.endDate()).toDays())
                        : Math.max(1, Duration.between(request.startDate(), request.endDate()).toHours());

                    BigDecimal vPrice = (request.rentalType() == RentalType.DAILY) ? vehiclePrice.getPricePerDay() : vehiclePrice.getPricePerHour();
                    BigDecimal dPrice = (request.rentalType() == RentalType.DAILY) ? driverPrice.getPricePerDay() : driverPrice.getPricePerHour();

                    BigDecimal subTotal = vPrice.add(dPrice).multiply(BigDecimal.valueOf(duration));
                    BigDecimal commission = subTotal.multiply(BigDecimal.valueOf(0.20));
                    BigDecimal deposit = subTotal.multiply(BigDecimal.valueOf(0.30));
                    BigDecimal total = subTotal.add(commission).add(deposit);

                    RentalEntity rental = RentalEntity.builder()
                        .id(UUID.randomUUID())
                        .clientId(null) // Walk-in : Pas de compte utilisateur
                        .clientName(request.clientName())
                        .clientPhone(request.clientPhone())
                        .agencyId(agencyId)
                        .vehicleId(request.vehicleId())
                        .driverId(request.driverId())
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .status(RentalStatus.PENDING)
                        .rentalType(request.rentalType())
                        .totalAmount(total)
                        .amountPaid(BigDecimal.ZERO)
                        .commissionAmount(commission)
                        .depositAmount(deposit)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .isNewRecord(true)
                        .build();

                    return rentalRepository.save(rental)
                        .flatMap(saved ->
                            // Notification : Agence uniquement (Client walk-in)
                            notifyStakeholders(saved, NotificationReason.RESERVATION,
                                null, // Pas de notif client
                                "Nouvelle location Walk-in créée pour " + request.clientName(),
                                null // Pas encore de notif chauffeur (attente paiement)
                            ).then(agencyRepository.findById(agencyId))
                            .map(agency -> new RentalInitResponse(
                                true, "Location agence créée.", saved.getId(), total, deposit, commission, agencyMapper.toDto(agency)
                            ))
                        );
                });
            });
    }

    /**
     * Démarrer la location (Client récupère le véhicule)
     */
    @Transactional
    public Mono<RentalEntity> startRental(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .filter(r -> r.getStatus() == RentalStatus.PAID)
            .switchIfEmpty(Mono.error(new RuntimeException("La location doit être payée pour démarrer.")))
            .flatMap(rental -> {
                rental.setStatus(RentalStatus.ONGOING);
                rental.setUpdatedAt(LocalDateTime.now());
                return rentalRepository.save(rental)
                    .flatMap(saved -> notifyStakeholders(saved, NotificationReason.LOCATION_START,
                        "Votre location a commencé. Bonne route !",
                        "Le véhicule est sorti. Location démarrée.",
                        "La course commence maintenant. Client à bord."
                    ).thenReturn(saved));
            });
    }

    /**
     * Client signale la fin de la location
     */
    @Transactional
    public Mono<RentalEntity> signalEndRental(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .filter(r -> r.getStatus() == RentalStatus.ONGOING)
            .flatMap(rental -> {
                rental.setStatus(RentalStatus.UNDER_REVIEW);
                rental.setUpdatedAt(LocalDateTime.now());
                return rentalRepository.save(rental)
                    .flatMap(saved -> notifyStakeholders(saved, NotificationReason.LOCATION_END_SIGNAL,
                        "Vous avez signalé le retour. En attente de validation par l'agence.",
                        "Le client a signalé le retour du véhicule. Veuillez valider l'état.",
                        "Course terminée (En attente validation agence)."
                    ).thenReturn(saved));
            });
    }

    /**
     * Agence valide le retour et déclenche la révision
     */
    @Transactional
    public Mono<RentalEntity> validateReturn(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .filter(r -> r.getStatus() == RentalStatus.UNDER_REVIEW)
            .flatMap(rental -> {
                rental.setStatus(RentalStatus.COMPLETED);
                rental.setUpdatedAt(LocalDateTime.now());

                LocalDateTime maintenanceEnd = rental.getEndDate().plusHours(24);

                Mono<Void> maintenanceBlock = rentalRepository.countConflictingRentals(rental.getVehicleId(), rental.getEndDate(), maintenanceEnd)
                    .flatMap(count -> {
                        if (count == 0) {
                            ScheduleRequestDTO schedule = new ScheduleRequestDTO(
                                rental.getEndDate(), maintenanceEnd, "MAINTENANCE", "Révision post-location"
                            );
                            return Mono.when(
                                scheduleService.addUnavailability(rental.getAgencyId(), ResourceType.VEHICLE, rental.getVehicleId(), schedule),
                                scheduleService.addUnavailability(rental.getAgencyId(), ResourceType.DRIVER, rental.getDriverId(), schedule)
                            );
                        }
                        return Mono.empty();
                    });

                return rentalRepository.save(rental)
                    .then(maintenanceBlock)
                    .flatMap(v -> notifyStakeholders(rental, NotificationReason.LOCATION_END,
                        "Retour validé. Merci pour votre confiance. À bientôt !",
                        "Retour validé. Véhicule en maintenance pour 24h.",
                        "Mission terminée avec succès. Véhicule rendu."
                    ).thenReturn(rental));
            });
    }

    public Flux<RentalEntity> getRentalsByAgency(UUID agencyId) {
        return rentalRepository.findAllByAgencyId(agencyId);
    }

    public Flux<RentalEntity> getRentalsByAgencyAndStatus(UUID agencyId, RentalStatus status) {
        return rentalRepository.findAllByAgencyIdAndStatus(agencyId, status);
    }

    // ========================================================================
    //  MÉTHODE CENTRALISÉE DE NOTIFICATION (CLIENT + AGENCE + CHAUFFEUR)
    // ========================================================================
    private Mono<Void> notifyStakeholders(RentalEntity rental, NotificationReason reason, String clientMsg, String agencyMsg, String driverMsg) {

        // 1. Notification Client (Seulement si compte existant)
        Mono<Void> clientNotify = Mono.empty();
        if (rental.getClientId() != null && clientMsg != null) {
            clientNotify = notificationService.createNotification(
                rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT, reason,
                rental.getVehicleId(), rental.getDriverId(), clientMsg
            ).then();
        }

        // 2. Notification Agence (Toujours)
        Mono<Void> agencyNotify = notificationService.createNotification(
            rental.getId(), rental.getAgencyId(), NotificationResourceType.AGENCY, reason,
            rental.getVehicleId(), rental.getDriverId(), agencyMsg
        ).then();

        // 3. Notification Chauffeur (Seulement si un chauffeur est assigné et message présent)
        Mono<Void> driverNotify = Mono.empty();
        if (rental.getDriverId() != null && driverMsg != null) {
            driverNotify = notificationService.createNotification(
                rental.getId(), rental.getDriverId(), NotificationResourceType.DRIVER, reason,
                rental.getVehicleId(), rental.getDriverId(), driverMsg
            ).then();
        }

        return Mono.when(clientNotify, agencyNotify, driverNotify);
    }
}
