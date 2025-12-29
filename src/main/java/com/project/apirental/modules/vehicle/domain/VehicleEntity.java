package com.project.apirental.modules.vehicle.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("vehicles")
public class VehicleEntity implements Persistable<UUID> {
    @Id private UUID id;
    private UUID organizationId;
    private UUID agencyId;
    private UUID categoryId;

    private String immatriculation;
    private String marque;
    private String modele;
    private Double kilometrage;
    private String transmission;
    private String couleur;
    private String carburantType;
    private Integer places;
    private Integer bagageCapacity;
    private Double puissance;

    private Boolean hasAirConditioner;
    private Integer childSeatCount;
    private Boolean hasWifi;
    private Boolean hasTv;
    private String gpsType;
    private Boolean hasBluetooth;
    private Boolean hasSeatBelt;

    @Builder.Default
    private String statut = "AVAILABLE"; // AVAILABLE, RENTED, MAINTENANCE, UNAVAILABLE
    private String imageUrl;
    private LocalDateTime createdAt;

    @Transient @Builder.Default @JsonIgnore private boolean isNewRecord = false;
    @Override public boolean isNew() { return isNewRecord || id == null; }
}