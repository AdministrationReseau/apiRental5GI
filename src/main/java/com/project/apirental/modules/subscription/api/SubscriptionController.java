package com.project.apirental.modules.subscription.api;

import com.project.apirental.modules.subscription.dto.PlanUpgradeRequest;
import com.project.apirental.modules.subscription.dto.SubscriptionResponseDTO;
import com.project.apirental.modules.subscription.mapper.SubscriptionMapper;
import com.project.apirental.modules.subscription.services.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Management")
// @SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionMapper subscriptionMapper;

    @Operation(summary = "Récupérer la souscription active d'une organisation")
    @GetMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> getMySubscription(@PathVariable UUID orgId) {
        return subscriptionService.getActiveSubscription(orgId)
                .map(subscriptionMapper::toDto)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour le plan d'une organisation")
    @PutMapping("/org/{orgId}/upgrade")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> upgradePlan(
            @PathVariable UUID orgId,
            @RequestBody PlanUpgradeRequest request) {
        return subscriptionService.upgradePlan(orgId, request.newPlan())
                .map(subscriptionMapper::toDto)
                .map(ResponseEntity::ok);
    }
}