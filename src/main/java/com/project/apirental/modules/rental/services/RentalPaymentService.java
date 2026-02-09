package com.project.apirental.modules.rental.services;

import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.notification.services.NotificationService;
import com.project.apirental.modules.rental.domain.PaymentEntity;
import com.project.apirental.modules.rental.domain.RentalEntity;
import com.project.apirental.modules.rental.dto.PaymentRequest;
import com.project.apirental.modules.rental.repository.PaymentRepository;
import com.project.apirental.modules.rental.repository.RentalRepository;
import com.project.apirental.modules.schedule.services.ScheduleService;
import com.project.apirental.shared.dto.ScheduleRequestDTO;
import com.project.apirental.shared.enums.NotificationReason;
import com.project.apirental.shared.enums.NotificationResourceType;
import com.project.apirental.shared.enums.RentalStatus;
import com.project.apirental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalPaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final AgencyRepository agencyRepository;
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;

    @Transactional
    public Mono<RentalEntity> processPayment(UUID rentalId, PaymentRequest request) {
        return rentalRepository.findById(rentalId)
            .switchIfEmpty(Mono.error(new RuntimeException("Location non trouvée")))
            .flatMap(rental -> {
                // 1. Enregistrer le paiement
                PaymentEntity payment = PaymentEntity.builder()
                    .id(UUID.randomUUID())
                    .rentalId(rentalId)
                    .amount(request.amount())
                    .paymentMethod(request.method())
                    .transactionDate(LocalDateTime.now())
                    .transactionRef("TXN-" + System.currentTimeMillis())
                    .isNewRecord(true)
                    .build();

                return paymentRepository.save(payment).flatMap(savedPayment -> {
                    // 2. Mise à jour du montant payé
                    BigDecimal newAmountPaid = rental.getAmountPaid().add(request.amount());
                    rental.setAmountPaid(newAmountPaid);

                    // 3. Logique d'état (60% / 100%)
                    BigDecimal total = rental.getTotalAmount();
                    BigDecimal sixtyPercent = total.multiply(BigDecimal.valueOf(0.6));

                    RentalStatus oldStatus = rental.getStatus();

                    if (newAmountPaid.compareTo(total) >= 0) {
                        rental.setStatus(RentalStatus.PAID);
                    } else if (newAmountPaid.compareTo(sixtyPercent) >= 0) {
                        rental.setStatus(RentalStatus.RESERVED);
                    }

                    // 4. Mise à jour des revenus de l'agence
                    Mono<Void> updateAgencyRevenue = agencyRepository.findById(rental.getAgencyId())
                        .flatMap(agency -> {
                            agency.setMonthlyRevenue(agency.getMonthlyRevenue() + request.amount().doubleValue());
                            return agencyRepository.save(agency);
                        }).then();

                    // 5. Blocage Planning (Si on passe à RESERVED ou PAID pour la première fois)
                    Mono<Void> blockSchedule = Mono.empty();
                    if (oldStatus == RentalStatus.PENDING && (rental.getStatus() == RentalStatus.RESERVED || rental.getStatus() == RentalStatus.PAID)) {
                        ScheduleRequestDTO schedule = new ScheduleRequestDTO(
                            rental.getStartDate(), rental.getEndDate(), "RENTED", "Location #" + rental.getId()
                        );
                        blockSchedule = Mono.when(
                            scheduleService.addUnavailability(rental.getAgencyId(), ResourceType.VEHICLE, rental.getVehicleId(), schedule),
                            scheduleService.addUnavailability(rental.getAgencyId(), ResourceType.DRIVER, rental.getDriverId(), schedule)
                        );
                    }

                    // 6. Notifications

                    // A. Notification Client (Si compte existe)
                    Mono<Void> notifyClient = Mono.empty();
                    if (rental.getClientId() != null) {
                        notifyClient = notificationService.createNotification(
                            rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT, NotificationReason.PAYMENT_RECEIVED,
                            rental.getVehicleId(), rental.getDriverId(),
                            "Paiement de " + request.amount() + " XAF reçu. Statut: " + rental.getStatus()
                        ).then();
                    }

                    // B. Notification Agence (Toujours)
                    Mono<Void> notifyAgency = notificationService.createNotification(
                        rental.getId(), rental.getAgencyId(), NotificationResourceType.AGENCY, NotificationReason.PAYMENT_RECEIVED,
                        rental.getVehicleId(), rental.getDriverId(),
                        "Paiement reçu (" + request.amount() + " XAF) pour la location #" + rental.getId()
                    ).then();

                    // C. Notification Chauffeur (Seulement si la réservation est confirmée pour la première fois)
                    Mono<Void> notifyDriver = Mono.empty();
                    if (oldStatus == RentalStatus.PENDING && (rental.getStatus() == RentalStatus.RESERVED || rental.getStatus() == RentalStatus.PAID)) {
                         notifyDriver = notificationService.createNotification(
                            rental.getId(), rental.getDriverId(), NotificationResourceType.DRIVER, NotificationReason.RESERVATION,
                            rental.getVehicleId(), rental.getDriverId(),
                            "Nouvelle course confirmée du " + rental.getStartDate() + " au " + rental.getEndDate()
                        ).then();
                    }

                    return updateAgencyRevenue
                        .then(blockSchedule)
                        .then(notifyClient)
                        .then(notifyAgency)
                        .then(notifyDriver)
                        .then(rentalRepository.save(rental));
                });
            });
    }
}
