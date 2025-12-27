package com.project.apirental.modules.subscription.services;

import com.project.apirental.modules.subscription.domain.SubscriptionCatalog;
import com.project.apirental.modules.subscription.domain.SubscriptionCatalog.SubscriptionPlan;
import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final OrganizationRepository organizationRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Vérifie si le plan a expiré et rétrograde vers FREE si nécessaire
     */
    public Mono<OrganizationEntity> checkAndDowngrade(OrganizationEntity org) {
        if (org.getSubscriptionExpiresAt() != null && org.getSubscriptionExpiresAt().isBefore(LocalDateTime.now())) {
            log.info("⏳ Plan expiré pour l'organisation {}. Rétrogradation vers FREE.", org.getName());
            org.setSubscriptionPlanName(SubscriptionCatalog.PLAN_FREE);
            org.setSubscriptionExpiresAt(null);
            return organizationRepository.save(org)
                    .doOnSuccess(saved -> eventPublisher.publishEvent(new AuditEvent("AUTO_DOWNGRADE", "SUBSCRIPTION", "Org " + org.getId() + " reverted to FREE")));
        }
        return Mono.just(org);
    }

    public Mono<Void> initializeDefaultSubscription(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .flatMap(org -> {
                    org.setSubscriptionPlanName(SubscriptionCatalog.PLAN_FREE);
                    org.setSubscriptionExpiresAt(null);
                    return organizationRepository.save(org);
                }).then();
    }

    public Mono<SubscriptionPlan> upgradePlan(UUID organizationId, String newPlanName) {
        SubscriptionPlan plan = SubscriptionCatalog.PLANS.get(newPlanName);
        if (plan == null) return Mono.error(new RuntimeException("Plan invalide"));

        return organizationRepository.findById(organizationId)
                .flatMap(org -> paymentService.processPayment(org.getEmail(), newPlanName, plan.price().doubleValue())
                        .flatMap(success -> {
                            org.setSubscriptionPlanName(newPlanName);
                            org.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(plan.durationDays()));
                            return organizationRepository.save(org).thenReturn(plan);
                        }));
    }
}