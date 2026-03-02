package com.project.apirental.modules.rental.services;

import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.notification.domain.NotificationTemplate;
import com.project.apirental.modules.notification.services.NotificationService;
import com.project.apirental.modules.rental.domain.PaymentEntity;
import com.project.apirental.modules.rental.domain.RentalEntity;
import com.project.apirental.modules.rental.dto.PaymentRequest;
import com.project.apirental.modules.rental.repository.PaymentRepository;
import com.project.apirental.modules.rental.repository.RentalRepository;
import com.project.apirental.modules.schedule.services.ScheduleService;
import com.project.apirental.shared.dto.ScheduleRequestDTO;
import com.project.apirental.shared.enums.*;
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
                // 1. Enregistrement du paiement
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

                    // 3. Logique des seuils (60% = RESERVED, 100% = PAID)
                    BigDecimal total = rental.getTotalAmount();
                    BigDecimal sixtyPercent = total.multiply(BigDecimal.valueOf(0.6));

                    RentalStatus oldStatus = rental.getStatus();
                    RentalStatus newStatus = oldStatus;

                    if (newAmountPaid.compareTo(total) >= 0) {
                        newStatus = RentalStatus.PAID;
                    } else if (newAmountPaid.compareTo(sixtyPercent) >= 0) {
                        newStatus = RentalStatus.RESERVED;
                    }

                    rental.setStatus(newStatus);

                    // 4. Mise à jour revenus Agence
                    Mono<Void> updateRevenue = agencyRepository.findById(rental.getAgencyId())
                        .flatMap(agency -> {
                            agency.setMonthlyRevenue(agency.getMonthlyRevenue() + request.amount().doubleValue());
                            return agencyRepository.save(agency);
                        }).then();

                    // 5. Blocage Planning (Si passage à RESERVED ou PAID pour la première fois)
                    Mono<Void> blockSchedule = Mono.empty();
                    if (oldStatus == RentalStatus.PENDING && (newStatus == RentalStatus.RESERVED || newStatus == RentalStatus.PAID)) {
                        ScheduleRequestDTO schedule = new ScheduleRequestDTO(
                            rental.getStartDate(), rental.getEndDate(), "RENTED", "Location #" + rental.getId()
                        );
                        blockSchedule = Mono.when(
                            scheduleService.addUnavailability(rental.getAgencyId(), ResourceType.VEHICLE, rental.getVehicleId(), schedule),
                            scheduleService.addUnavailability(rental.getAgencyId(), ResourceType.DRIVER, rental.getDriverId(), schedule)
                        );
                    }

                    // 6. Notifications (Utilisation des Templates)
                    Mono<Void> notifyClient = (rental.getClientId() != null) ? notificationService.createNotification(
                        rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT, NotificationReason.PAYMENT_RECEIVED,
                        rental.getVehicleId(), rental.getDriverId(),
                        NotificationTemplate.PAYMENT_RECEIVED_CLIENT, request.amount(), newAmountPaid, total, newStatus
                    ).then() : Mono.empty();

                    Mono<Void> notifyAgency = notificationService.createNotification(
                        rental.getId(), rental.getAgencyId(), NotificationResourceType.AGENCY, NotificationReason.PAYMENT_RECEIVED,
                        rental.getVehicleId(), rental.getDriverId(),
                        NotificationTemplate.PAYMENT_RECEIVED_AGENCY, request.amount(), rental.getId()
                    ).then();

                    // Notification spécifique "Réservation Réussie" (Passage à RESERVED)
                    Mono<Void> notifyReservationSuccess = Mono.empty();
                    if (oldStatus == RentalStatus.PENDING && newStatus == RentalStatus.RESERVED) {
                        notifyReservationSuccess = Mono.when(
                            notificationService.createNotification(
                                rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT, NotificationReason.RESERVATION_CREATED,
                                rental.getVehicleId(), rental.getDriverId(),
                                NotificationTemplate.RESERVATION_CONFIRMED_CLIENT, rental.getId()
                            ),
                            notificationService.createNotification(
                                rental.getId(), rental.getAgencyId(), NotificationResourceType.AGENCY, NotificationReason.RESERVATION_CREATED,
                                rental.getVehicleId(), rental.getDriverId(),
                                NotificationTemplate.RESERVATION_CONFIRMED_AGENCY, rental.getId(), rental.getClientId()
                            ),
                            // Notif Chauffeur
                            notificationService.createNotification(
                                rental.getId(), rental.getDriverId(), NotificationResourceType.DRIVER, NotificationReason.RESERVATION_CREATED,
                                rental.getVehicleId(), rental.getDriverId(),
                                NotificationTemplate.RESERVATION_CONFIRMED_DRIVER, rental.getStartDate(), rental.getEndDate()
                            )
                        );
                    }

                    return updateRevenue
                        .then(blockSchedule)
                        .then(notifyClient)
                        .then(notifyAgency)
                        .then(notifyReservationSuccess)
                        .then(rentalRepository.save(rental));
                });
            });
    }
}
