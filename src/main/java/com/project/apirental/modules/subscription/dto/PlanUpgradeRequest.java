package com.project.apirental.modules.subscription.dto;

import com.project.apirental.modules.subscription.domain.PlanType;
import jakarta.validation.constraints.NotNull;

public record PlanUpgradeRequest(
    @NotNull PlanType newPlan
) {}