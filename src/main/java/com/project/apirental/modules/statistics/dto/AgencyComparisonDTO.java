package com.project.apirental.modules.statistics.dto;

import java.math.BigDecimal;

public record AgencyComparisonDTO(
    String agencyName,
    Long totalVehicles,
    Long totalRentals,
    BigDecimal revenue
) {}
