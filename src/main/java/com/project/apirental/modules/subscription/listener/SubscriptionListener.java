package com.project.apirental.modules.subscription.listener;

import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionListener {

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Async
    public void onSubscriptionChanged(AuditEvent event) {
        if ("UPDATE_PLAN".equals(event.action())) {
            log.info("🔥 Plan Upgrade detected: {}", event.details());
            // Ici, on pourrait déclencher l'envoi d'un email de confirmation
        }
    }
}