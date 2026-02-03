package com.project.apirental.modules.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

// DTO utilisé pour mettre à jour les informations éditables par l'utilisateur
public record OrgUpdateDTO(
    @NotNull
    String name,
    @NotNull
    String description,
    @NotNull
    String address,
    @NotNull
    String city,
    @NotNull
    String postalCode,
    @NotNull
    String region,
    @NotNull
    String phone,
    @NotNull
    @Email
    String email,
    @NotNull
    String website,
    @NotNull
    String timezone,
    @NotNull
    String logoUrl,
    @NotNull
    String registrationNumber,
    @NotNull
    String taxNumber,
    @NotNull
    Boolean isDriverBookingRequired
) {}
