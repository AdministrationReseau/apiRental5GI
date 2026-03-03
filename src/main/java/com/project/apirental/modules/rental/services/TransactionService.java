package com.project.apirental.modules.rental.services;

import com.project.apirental.modules.rental.dto.TransactionDetailResponseDTO;
import com.project.apirental.modules.rental.dto.TransactionResponseDTO;
import com.project.apirental.modules.rental.repository.PaymentRepository;
import com.project.apirental.modules.rental.repository.RentalRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final RentalRepository rentalRepository;

    // Injection du RentalService pour récupérer les détails de la location liée au paiement
    private final RentalService rentalService;

    // =================================================================================
    // NOUVELLE MÉTHODE : Obtenir les détails complets d'une transaction
    // =================================================================================
    public Mono<TransactionDetailResponseDTO> getTransactionDetails(UUID transactionId) {
        // 1. On cherche d'abord si c'est un paiement de location
        Mono<TransactionDetailResponseDTO> paymentMono = paymentRepository.findById(transactionId)
            .flatMap(payment -> rentalService.getRentalDetails(payment.getRentalId())
                .map(rentalDetails -> new TransactionDetailResponseDTO(
                    payment.getId(),
                    "RENTAL_PAYMENT",
                    payment.getAmount(),
                    "Paiement Location #" + payment.getRentalId().toString().substring(0, 8),
                    payment.getTransactionDate(),
                    payment.getTransactionRef(),
                    "COMPLETED",
                    payment.getPaymentMethod(),
                    rentalDetails,
                    null
                )));

        // 2. Si ce n'est pas un paiement, on cherche si c'est un paiement d'abonnement
        Mono<TransactionDetailResponseDTO> subMono = subscriptionRepository.findById(transactionId)
            .flatMap(sub -> planRepository.findByName(sub.getPlanType())
                .map(plan -> new TransactionDetailResponseDTO(
                    sub.getId(),
                    "SUBSCRIPTION_COST",
                    plan.getPrice().negate(),
                    "Abonnement " + plan.getName(),
                    sub.getStartDate(),
                    "SUB-" + sub.getId().toString().substring(0, 8),
                    sub.getStatus(),
                    null,
                    null,
                    plan
                )));

        // On retourne l'un ou l'autre, ou une erreur si introuvable
        return paymentMono.switchIfEmpty(subMono)
            .switchIfEmpty(Mono.error(new RuntimeException("Transaction non trouvée")));
    }

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
