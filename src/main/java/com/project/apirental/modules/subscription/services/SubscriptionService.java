package com.project.apirental.modules.subscription.services;

import com.project.apirental.modules.subscription.domain.SubscriptionEntity;
import com.project.apirental.modules.subscription.domain.SubscriptionPlanEntity;
import com.project.apirental.modules.subscription.dto.SubscriptionRemainingTimeDTO;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionRepository; // Import correct
import com.project.apirental.modules.auth.dto.AuthResponse;
import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository; // FIX : Utiliser le bon type ici
    private final OrganizationRepository organizationRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Mono<Void> initializeDefaultSubscription(UUID organizationId) {
        return planRepository.findByName("FREE")
                .switchIfEmpty(Mono.error(new RuntimeException("Plan FREE non configuré")))
                .flatMap(plan -> organizationRepository.findById(organizationId)
                        .flatMap(org -> {
                            // 1. Mise à jour de l'état de l'organisation
                            org.setSubscriptionPlanId(plan.getId());
                            org.setSubscriptionExpiresAt(null);
                            org.setSubscriptionAutoRenew(true);

                            // 2. Création de l'enregistrement historique
                            SubscriptionEntity subRecord = SubscriptionEntity.builder()
                                    .id(UUID.randomUUID())
                                    .organizationId(organizationId)
                                    .planType(plan.getName())
                                    .status("ACTIVE")
                                    .startDate(LocalDateTime.now())
                                    .isNewRecord(true)
                                    .build();

                            // 3. On chaîne les deux sauvegardes pour qu'elles s'exécutent
                            return organizationRepository.save(org)
                                    .then(subscriptionRepository.save(subRecord))
                                    .doOnSuccess(s -> log.info("✅ Historique de souscription créé pour l'org {}", organizationId));
                        }))
                .then();
    }

    @Transactional
    public Mono<SubscriptionPlanEntity> upgradePlan(UUID organizationId, String planName) {
        return planRepository.findByName(planName)
                .flatMap(plan -> organizationRepository.findById(organizationId)
                        .flatMap(org -> paymentService.processPayment(org.getEmail(), planName, plan.getPrice().doubleValue())
                                .flatMap(success -> {
                                    org.setSubscriptionPlanId(plan.getId());
                                    org.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(plan.getDurationDays()));
                                    
                                    SubscriptionEntity subRecord = SubscriptionEntity.builder()
                                            .id(UUID.randomUUID())
                                            .organizationId(organizationId)
                                            .planType(plan.getName())
                                            .status("ACTIVE")
                                            .startDate(LocalDateTime.now())
                                            .endDate(org.getSubscriptionExpiresAt())
                                            .isNewRecord(true)
                                            .build();

                                    return organizationRepository.save(org)
                                            .then(subscriptionRepository.save(subRecord))
                                            .thenReturn(plan);
                                })));
    }

    @Transactional
    public Mono<OrganizationEntity> checkAndDowngrade(OrganizationEntity org) {
        if (org.getSubscriptionExpiresAt() != null && org.getSubscriptionExpiresAt().isBefore(LocalDateTime.now())) {
            return planRepository.findByName("FREE")
                    .flatMap(freePlan -> {
                        org.setSubscriptionPlanId(freePlan.getId());
                        org.setSubscriptionExpiresAt(null);
                        
                        SubscriptionEntity history = SubscriptionEntity.builder()
                                .id(UUID.randomUUID())
                                .organizationId(org.getId())
                                .planType("FREE")
                                .status("AUTO_DOWNGRADE")
                                .startDate(LocalDateTime.now())
                                .isNewRecord(true)
                                .build();

                        return subscriptionRepository.save(history)
                                .then(organizationRepository.save(org));
                    });
        }
        return Mono.just(org);
    }

    public Mono<SubscriptionRemainingTimeDTO> getRemainingTime(UUID orgId) {
        return organizationRepository.findById(orgId)
                .flatMap(this::checkAndDowngrade)
                .map(org -> {
                    if (org.getSubscriptionExpiresAt() == null) {
                        return new SubscriptionRemainingTimeDTO(0, 0, 0, "Illimité (Plan FREE)", true);
                    }
                    Duration d = Duration.between(LocalDateTime.now(), org.getSubscriptionExpiresAt());
                    return new SubscriptionRemainingTimeDTO(d.toDays(), d.toHoursPart(), d.toMinutesPart(), 
                            d.toDays() + " jours restants", false);
                });
    }

    public Mono<SubscriptionEntity> createHistoryRecord(UUID organizationId, String planName, LocalDateTime endDate) {
        SubscriptionEntity history = SubscriptionEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .planType(planName)
                .status("ACTIVE")
                .startDate(LocalDateTime.now())
                .endDate(endDate)
                .isNewRecord(true)
                .build();
                
        return subscriptionRepository.save(history);
    }
}