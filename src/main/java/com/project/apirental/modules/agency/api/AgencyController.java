package com.project.apirental.modules.agency.api;

import com.project.apirental.modules.agency.dto.AgencyRequestDTO;
import com.project.apirental.modules.agency.dto.AgencyResponseDTO;
import com.project.apirental.modules.agency.services.AgencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/agencies")
@RequiredArgsConstructor
@Tag(name = "Agency Management", description = "CRUD pour les agences des organisations")
@SecurityRequirement(name = "bearerAuth")
public class AgencyController {

    private final AgencyService agencyService;

    
    @Operation(summary = "Lister les agences d'une organisation")
    @GetMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('ADMIN')")
    public Flux<AgencyResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return agencyService.getAgenciesByOrg(orgId);
    }

    @Operation(summary = "Obtenir les détails d'une agence")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZATION', 'ADMIN', 'AGENT')")
    public Mono<ResponseEntity<AgencyResponseDTO>> getById(@PathVariable UUID id) {
        return agencyService.getAgency(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Créer une nouvelle agence pour une organisation")
     @PostMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<AgencyResponseDTO>> create(@PathVariable UUID orgId, @RequestBody AgencyRequestDTO request) {
        return agencyService.createAgency(orgId, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Modifier une agence")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<AgencyResponseDTO>> update(@PathVariable UUID id, @RequestBody AgencyRequestDTO request) {
        return agencyService.updateAgency(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer une agence")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return agencyService.deleteAgency(id).then(Mono.just(ResponseEntity.noContent().build()));
    }
}