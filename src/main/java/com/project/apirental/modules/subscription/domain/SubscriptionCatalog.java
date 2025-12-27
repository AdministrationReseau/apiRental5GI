package com.project.apirental.modules.subscription.domain;

import java.math.BigDecimal;
import java.util.Map;

public class SubscriptionCatalog {
    public static final String PLAN_FREE = "FREE";
    public static final String PLAN_PRO = "PRO";
    public static final String PLAN_ENTERPRISE = "ENTERPRISE";

    public static final Map<String, SubscriptionPlan> PLANS = Map.of(
        PLAN_FREE, new SubscriptionPlan(PLAN_FREE, "Plan de base", BigDecimal.ZERO, 0, 5, 5, 1, 2, false, false),
        PLAN_PRO, new SubscriptionPlan(PLAN_PRO, "Plan Pro", new BigDecimal("29.99"), 30, 50, 50, 10, 30, true, true),
        PLAN_ENTERPRISE, new SubscriptionPlan(PLAN_ENTERPRISE, "Plan Entreprise", new BigDecimal("99.99"), 30, 999, 999, 999, 999, true, true)
    );

    // C'est ce Record qui définit la structure d'un plan
    public record SubscriptionPlan(
        String name,
        String description,
        BigDecimal price,
        Integer durationDays,
        Integer maxVehicles,
        Integer maxDrivers,
        Integer maxAgencies,
        Integer maxUsers,
        Boolean hasGeofencing,
        Boolean hasChat
    ) {}
}