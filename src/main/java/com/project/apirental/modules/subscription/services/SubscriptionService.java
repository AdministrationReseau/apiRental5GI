package com.project.apirental.modules.subscription.services;

import com.project.apirental.modules.subscription.domain.PlanType;
import com.project.apirental.modules.subscription.domain.SubscriptionEntity;
import com.project.apirental.modules.subscription.repository.SubscriptionRepository;
import com.project.apirental.modules.auth.repository.UserRepository;
import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    public Mono<SubscriptionEntity> initializeDefaultSubscription(UUID organizationId) {
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .planType(PlanType.FREE.name())
                .status("ACTIVE")
                .startDate(LocalDateTime.now())
                .endDate(null) // Plan FREE = À vie
                .isNewRecord(true)
                .build();

        return subscriptionRepository.save(subscription)
                .doOnSuccess(s -> eventPublisher.publishEvent(new AuditEvent("CREATE_SUBSCRIPTION", "SUBSCRIPTION", "Default FREE plan (Lifetime) for org: " + organizationId)));
    }

    public Mono<SubscriptionEntity> getActiveSubscription(UUID organizationId) {
        return subscriptionRepository.findByOrganizationIdAndStatus(organizationId, "ACTIVE")
                .switchIfEmpty(Mono.error(new RuntimeException("No active subscription found")));
    }

    @Transactional
    public Mono<SubscriptionEntity> upgradePlan(UUID organizationId, PlanType newPlan) {
        // 1. Récupérer la souscription actuelle
        return subscriptionRepository.findByOrganizationIdAndStatus(organizationId, "ACTIVE")
                .flatMap(subscription -> {
                    // 2. Si c'est un plan payant, on simule le paiement
                    if (newPlan == PlanType.PRO || newPlan == PlanType.ENTERPRISE) {
                        double price = (newPlan == PlanType.PRO) ? 29.99 : 99.99;
                        
                        return paymentService.processPayment("owner@system.com", newPlan.name(), price)
                                .flatMap(paymentSuccess -> {
                                    if (!paymentSuccess) return Mono.error(new RuntimeException("Payment failed"));
                                    
                                    // 3. Mise à jour avec durée de 30 jours
                                    subscription.setPlanType(newPlan.name());
                                    subscription.setStartDate(LocalDateTime.now());
                                    subscription.setEndDate(LocalDateTime.now().plusDays(30));
                                    
                                    return subscriptionRepository.save(subscription);
                                });
                    } else {
                        // Retour au plan FREE
                        subscription.setPlanType(PlanType.FREE.name());
                        subscription.setEndDate(null);
                        return subscriptionRepository.save(subscription);
                    }
                })
                .doOnSuccess(s -> eventPublisher.publishEvent(new AuditEvent(
                        "UPGRADE_PLAN", 
                        "SUBSCRIPTION", 
                        "Org " + organizationId + " upgraded to " + newPlan + " until " + s.getEndDate()
                )));
    }
}