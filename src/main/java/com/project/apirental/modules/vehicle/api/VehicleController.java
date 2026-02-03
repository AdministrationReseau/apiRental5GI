package com.project.apirental.modules.vehicle.api;

import com.project.apirental.modules.vehicle.dto.VehicleRequestDTO;
import com.project.apirental.modules.vehicle.dto.VehicleResponseDTO;
import com.project.apirental.modules.vehicle.services.VehicleService;
import com.project.apirental.modules.vehicle.dto.VehicleDetailResponseDTO;
import com.project.apirental.modules.vehicle.dto.UpdateVehicleStatusDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle Management")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "Ajouter un véhicule à la flotte (Vérifie les quotas)")
    @NotNull
    @PostMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#orgId, 'vehicle:create')")
    public Mono<ResponseEntity<VehicleResponseDTO>> create(@PathVariable UUID orgId, @RequestBody VehicleRequestDTO request) {
        return vehicleService.createVehicle(orgId, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister tous les véhicules d'une organisation")
    @GetMapping("/org/{orgId}")
    // @PreAuthorize("hasRole('ORGANIZATION') or hasRole('ADMIN')")
    public Flux<VehicleResponseDTO> getAllByOrg(@PathVariable UUID orgId) {
        return vehicleService.getVehiclesByOrg(orgId);
    }

    @Operation(summary = "Lister les véhicules d'une agence")
    @GetMapping("/agency/{agencyId}")
    public Flux<VehicleResponseDTO> getAllByAgency(@PathVariable UUID agencyId) {
        return vehicleService.getVehiclesByAgency(agencyId);
    }

    @Operation(summary = "Lister tous les véhicules disponibles sur la plateforme")
    @GetMapping("/available")
    public Flux<VehicleResponseDTO> getAvailableVehicles() {
        return vehicleService.getAvailableVehicles();
    }

    @Operation(summary = "Obtenir les détails complets (Planning + Prix) d'un véhicule")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<VehicleDetailResponseDTO>> getVehicleDetails(@PathVariable UUID id) {
        return vehicleService.getVehicleDetails(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour statut (Maintenance/Indispo), planning et prix")
    @PatchMapping("/{id}/status-pricing")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<VehicleDetailResponseDTO>> updateStatusAndPricing(
            @PathVariable UUID id,
            @RequestBody UpdateVehicleStatusDTO request) {
        return vehicleService.updateVehicleStatusAndPricing(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtenir les détails d'un véhicule par son ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<VehicleResponseDTO>> getById(@PathVariable UUID id) {
        return vehicleService.getVehicleById(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour les informations d'un véhicule")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT') or @rbac.hasPermission(#orgId, 'vehicle:update')")
    public Mono<ResponseEntity<VehicleResponseDTO>> update(@PathVariable UUID id, @RequestBody VehicleRequestDTO request) {
        return vehicleService.updateVehicle(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Changer le statut du véhicule (MAINTENANCE, AVAILABLE, RENTED)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT') or @rbac.hasPermission(#orgId, 'vehicle:update')")
    public Mono<ResponseEntity<VehicleResponseDTO>> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return vehicleService.updateVehicleStatus(id, status).map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer un véhicule (Met à jour les compteurs)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#orgId, 'vehicle:delete')")
     public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return vehicleService.deleteVehicle(id).then(Mono.just(ResponseEntity.noContent().build()));
    }

    @Operation(summary = "Lister les véhicules d'une organisation filtrés par catégorie")
    @GetMapping("/org/{orgId}/category/{categoryId}")
    @PreAuthorize("@rbac.hasPermission(#orgId, 'vehicle:list')")
    public Flux<VehicleResponseDTO> getByOrgAndCategory(
            @PathVariable UUID orgId, 
            @PathVariable UUID categoryId) {
        return vehicleService.getVehiclesByOrgAndCategory(orgId, categoryId);
    }

    @Operation(summary = "Lister les véhicules d'une agence filtrés par catégorie")
    @GetMapping("/agency/{agencyId}/category/{categoryId}")
    @PreAuthorize("@rbac.canAccessAgency(#agencyId, 'vehicle:list')")
    public Flux<VehicleResponseDTO> getByAgencyAndCategory(
            @PathVariable UUID agencyId, 
            @PathVariable UUID categoryId) {
        return vehicleService.getVehiclesByAgencyAndCategory(agencyId, categoryId);
    }
}
