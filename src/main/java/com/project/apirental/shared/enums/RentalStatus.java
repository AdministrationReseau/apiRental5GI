package com.project.apirental.shared.enums;

public enum RentalStatus {
    PENDING,        // Créé, en attente de paiement
    RESERVED,       // Acompte (60%) payé
    PAID,           // Totalité payée, prêt à démarrer
    ONGOING,        // Location en cours (Véhicule récupéré)
    COMPLETED,      // Véhicule rendu et validé par l'agence
    CANCELLED,      // Annulé
    UNDER_REVIEW    // Véhicule rendu par le client, en attente validation agence
}
