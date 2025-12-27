package com.project.apirental.modules.subscription.dto;

import com.project.apirental.modules.subscription.domain.PlanType;
import jakarta.validation.constraints.NotNull;

// Utilise l'Enum PlanType (FREE, PRO, ENTERPRISE)
public record PlanUpgradeRequest(
    @NotNull PlanType newPlan
) {}