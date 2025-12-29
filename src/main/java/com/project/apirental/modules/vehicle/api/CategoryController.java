package com.project.apirental.modules.vehicle.api;

import com.project.apirental.modules.vehicle.domain.VehicleCategoryEntity;
import com.project.apirental.modules.vehicle.dto.CategoryRequestDTO;
import com.project.apirental.modules.vehicle.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles/categories")
@RequiredArgsConstructor
@Tag(name = "Vehicle Category Management", description = "Gestion des catégories de véhicules (SUV, Berline, etc.)")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @Operation(summary = "Créer une catégorie de véhicule")
    @PostMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<VehicleCategoryEntity>> create(
            @PathVariable UUID orgId,
            @RequestBody CategoryRequestDTO request) { // Utilisation du DTO ici

        // C'est le serveur qui construit l'objet complet
        VehicleCategoryEntity category = VehicleCategoryEntity.builder()
                .id(UUID.randomUUID()) // Généré par le serveur
                .organizationId(orgId) // Récupéré de l'URL
                .name(request.name()) // Récupéré du DTO
                .description(request.description())
                .isNewRecord(true)
                .build();

        return categoryRepository.save(Objects.requireNonNull(category)).map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les véhicules d'une organisation (Org + Système)")
    @GetMapping("/org/{orgId}")
    public Flux<VehicleCategoryEntity> getByOrg(@PathVariable UUID orgId) {
        return categoryRepository.findAllByOrganizationIdOrSystem(orgId);
    }


    @Operation(summary = "Lister toutes les catégories utilisables par une agence (Org + Système)")
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasAnyRole('ORGANIZATION', 'ADMIN', 'AGENT')")
    public Flux<VehicleCategoryEntity> getByAgency(@PathVariable UUID agencyId) {
        return categoryRepository.findAllByAgencyIdOrSystem(agencyId);
    }

    @Operation(summary = "Obtenir les détails d'une catégorie")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<VehicleCategoryEntity>> getById(@PathVariable UUID id) {
        return categoryRepository.findById(Objects.requireNonNull(id))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new RuntimeException("Catégorie non trouvée")));
    }

    @Operation(summary = "Supprimer une catégorie (Uniquement si elle appartient à l'organisation)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return categoryRepository.findById(Objects.requireNonNull(id))
                .flatMap(cat -> {
                    if (cat.getOrganizationId() == null) {
                        return Mono.error(new RuntimeException("Impossible de supprimer une catégorie système"));
                    }
                    return categoryRepository.deleteById(Objects.requireNonNull(id));
                })
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}