package com.project.apirental.modules.rental.dto;

import com.project.apirental.shared.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentRequest(
    @NotNull BigDecimal amount,
    @NotNull PaymentMethod method
) {}
