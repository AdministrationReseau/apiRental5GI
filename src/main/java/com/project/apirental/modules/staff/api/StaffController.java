package com.project.apirental.modules.staff.api;

import com.project.apirental.modules.staff.dto.*;
import com.project.apirental.modules.staff.services.StaffService;
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
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Management", description = "Gestion du personnel")
@SecurityRequirement(name = "bearerAuth")
public class StaffController {

    private final StaffService staffService;

    @Operation(summary = "Ajouter un membre au staff d'une organisation")
    @PostMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<StaffResponseDTO>> create(@PathVariable UUID orgId, @RequestBody StaffRequestDTO request) {
        return staffService.addStaffToOrganization(orgId, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister tout le staff d'une organisation")
    @GetMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('ADMIN')")
    public Flux<StaffResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return staffService.getStaffByOrganization(orgId);
    }

    @Operation(summary = "Lister le staff d'une agence")
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Flux<StaffResponseDTO> getByAgency(@PathVariable UUID agencyId) {
        return staffService.getStaffByAgency(agencyId);
    }

    @Operation(summary = "Obtenir les détails d'un membre du staff")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<StaffResponseDTO>> getById(@PathVariable UUID id) {
        return staffService.getStaffById(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Modifier un membre du staff (Poste, Agence, Statut)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<StaffResponseDTO>> update(@PathVariable UUID id, @RequestBody StaffUpdateDTO request) {
        return staffService.updateStaff(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer un membre du staff")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return staffService.deleteStaff(id).then(Mono.just(ResponseEntity.noContent().build()));
    }
}