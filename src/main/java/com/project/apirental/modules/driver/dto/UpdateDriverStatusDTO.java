package com.project.apirental.modules.driver.dto;

import com.project.apirental.shared.dto.ScheduleRequestDTO;
import java.math.BigDecimal;

public record UpdateDriverStatusDTO(
    String globalStatus, // ACTIVE, INACTIVE, RENTED
    ScheduleRequestDTO schedule, // Optionnel : Si on veut ajouter une plage d'indisponibilité
    BigDecimal pricePerHour, // Optionnel : Mise à jour du prix
    BigDecimal pricePerDay
) {}
