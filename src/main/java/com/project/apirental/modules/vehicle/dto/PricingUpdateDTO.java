package com.project.apirental.modules.vehicle.dto;

import java.math.BigDecimal;

public record PricingUpdateDTO(
    BigDecimal pricePerHour,
    BigDecimal pricePerDay
) {}
