package com.project.apirental.modules.vehicle.dto;

import java.util.UUID;

public record VehicleResponseDTO(
    UUID id,
    UUID agencyId,
    UUID categoryId,
    String immatriculation,
    String marque,
    String modele,
    Double kilometrage,
    String transmission,
    String couleur,
    String carburantType,
    Integer places,
    Integer bagageCapacity,
    Double puissance,
    Boolean hasAirConditioner,
    Boolean hasWifi,
    Boolean hasTv,
    String gpsType,
    String imageUrl,
    String statut
) {}