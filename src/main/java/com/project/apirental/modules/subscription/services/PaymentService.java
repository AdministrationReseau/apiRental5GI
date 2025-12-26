package com.project.apirental.modules.subscription.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
@Slf4j
public class PaymentService {

    /**
     * Simule un appel à une passerelle de paiement (Stripe, etc.)
     */
    public Mono<Boolean> processPayment(String email, String planType, double amount) {
        log.info("💳 Simulation du paiement pour {} (Plan: {}, Montant: {}€)", email, planType, amount);
        
        // Simule un délai réseau de 800ms sans bloquer le thread
        return Mono.delay(Duration.ofMillis(800))
                .map(d -> {
                    log.info("✅ Paiement validé par la passerelle pour {}", email);
                    return true;
                });
    }
}