package com.project.apirental.modules.organization.dto;
// DTO utilisé pour mettre à jour les informations éditables par l'utilisateur
public record OrgUpdateDTO(
    String name,
    String description,
    String address,
    String city,
    String postalCode,
    String region,
    String phone,
    String email,
    String website,
    String timezone,
    String logoUrl,
    String registrationNumber,
    String taxNumber
) {}
