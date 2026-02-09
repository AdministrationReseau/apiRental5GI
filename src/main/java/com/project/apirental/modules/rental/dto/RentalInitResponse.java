package com.project.apirental.modules.rental.dto;

import com.project.apirental.modules.agency.dto.AgencyResponseDTO;
import java.math.BigDecimal;
import java.util.UUID;

public record RentalInitResponse(
    boolean isAllowed, // True si chauffeur inclus, False sinon
    String message,
    UUID rentalId, // Null si isAllowed est false
    BigDecimal totalAmount,
    BigDecimal depositAmount,
    BigDecimal commissionAmount,
    AgencyResponseDTO agencyDetails // Pour contacter l'agence si refusé
) {}
