package com.project.apirental.modules.vehicle.mapper;

import com.project.apirental.modules.vehicle.domain.VehicleCategoryEntity;
import com.project.apirental.modules.vehicle.domain.VehicleEntity;
import com.project.apirental.modules.vehicle.dto.VehicleResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public VehicleResponseDTO toDto(VehicleEntity vehicle, VehicleCategoryEntity category) {
        if (vehicle == null) return null;
        
        return new VehicleResponseDTO(
            vehicle.getId(),
            vehicle.getAgencyId(),
            vehicle.getCategoryId(),
            vehicle.getImmatriculation(),
            vehicle.getMarque(),
            vehicle.getModele(),
            vehicle.getKilometrage(),
            // category != null ? category.getName() : "Non classé",
            vehicle.getTransmission(),
            vehicle.getCouleur(),  
            vehicle.getCarburantType(),
            vehicle.getPlaces(),
            vehicle.getBagageCapacity(),
            vehicle.getPuissance(),
            vehicle.getHasAirConditioner(),
            vehicle.getHasWifi(),
            vehicle.getHasTv(),
            vehicle.getGpsType(),
            vehicle.getImageUrl(),
            vehicle.getStatut()
        );
    }
}