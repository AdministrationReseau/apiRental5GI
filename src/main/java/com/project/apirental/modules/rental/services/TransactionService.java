package com.project.apirental.modules.rental.services;

import com.project.apirental.modules.rental.dto.TransactionResponseDTO;
import com.project.apirental.modules.rental.repository.PaymentRepository;
import com.project.apirental.modules.rental.repository.RentalRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final RentalRepository rentalRepository;

    /**
     * Transactions d'un Client (Uniquement ses paiements de location)
     */
    public Flux<TransactionResponseDTO> getClientTransactions(UUID clientId) {
        return paymentRepository.findAllByClientId(clientId)
            .flatMap(payment -> rentalRepository.findById(payment.getRentalId())
                .map(rental -> new TransactionResponseDTO(
                    payment.getId(),
                    "RENTAL_PAYMENT",
                    payment.getAmount(),
                    "Paiement Location #" + rental.getId().toString().substring(0, 8),
                    payment.getTransactionDate(),
                    payment.getTransactionRef(),
                    "COMPLETED",
                    payment.getPaymentMethod()
                )));
    }

    /**
     * Transactions d'une Agence (Uniquement les revenus locatifs)
     */
    public Flux<TransactionResponseDTO> getAgencyTransactions(UUID agencyId) {
        return paymentRepository.findAllByAgencyId(agencyId)
            .flatMap(payment -> rentalRepository.findById(payment.getRentalId())
                .map(rental -> new TransactionResponseDTO(
                    payment.getId(),
                    "RENTAL_PAYMENT",
                    payment.getAmount(),
                    "Revenu Location #" + rental.getId().toString().substring(0, 8),
                    payment.getTransactionDate(),
                    payment.getTransactionRef(),
                    "COMPLETED",
                    payment.getPaymentMethod()
                )));
    }

    /**
     * Transactions d'une Organisation (Revenus Locatifs - Coûts Abonnements)
     * Fusionne les flux et trie par date décroissante.
     */
    public Flux<TransactionResponseDTO> getOrganizationTransactions(UUID orgId) {

        // 1. Flux des revenus locatifs (Positif)
        Flux<TransactionResponseDTO> rentalIncomeFlux = paymentRepository.findAllRentalPaymentsByOrganizationId(orgId)
            .map(payment -> new TransactionResponseDTO(
                payment.getId(),
                "RENTAL_INCOME",
                payment.getAmount(),
                "Revenu Location (Ref: " + payment.getTransactionRef() + ")",
                payment.getTransactionDate(),
                payment.getTransactionRef(),
                "COMPLETED",
                payment.getPaymentMethod()
            ));

        // 2. Flux des dépenses d'abonnement (Négatif ou Informatif)
        Flux<TransactionResponseDTO> subscriptionExpenseFlux = subscriptionRepository.findAllByOrganizationIdOrderByStartDateDesc(orgId)
            .flatMap(sub -> planRepository.findByName(sub.getPlanType())
                .map(plan -> new TransactionResponseDTO(
                    sub.getId(),
                    "SUBSCRIPTION_COST",
                    plan.getPrice().negate(), // On met en négatif pour indiquer une dépense
                    "Abonnement " + plan.getName(),
                    sub.getStartDate(),
                    "SUB-" + sub.getId().toString().substring(0, 8),
                    sub.getStatus(),
                    null // Méthode de paiement non stockée dans l'historique sub pour l'instant
                )));

        // 3. Fusion et Tri
        return Flux.merge(rentalIncomeFlux, subscriptionExpenseFlux)
            .sort(Comparator.comparing(TransactionResponseDTO::date).reversed());
    }
}
