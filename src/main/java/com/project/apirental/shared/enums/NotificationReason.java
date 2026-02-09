package com.project.apirental.shared.enums;

public enum NotificationReason {
    RESERVATION,          // Création de la demande
    PAYMENT_RECEIVED,     // Paiement effectué
    LOCATION_START,       // Début effectif (récupération véhicule)
    LOCATION_END_SIGNAL,  // Client signale le retour
    LOCATION_END,         // Agence valide le retour (Fin définitive)
    MAINTENANCE_SCHEDULED // Maintenance auto après location
}
