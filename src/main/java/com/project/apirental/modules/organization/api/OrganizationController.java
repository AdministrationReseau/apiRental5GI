package com.project.apirental.modules.organization.api;

import com.project.apirental.modules.organization.dto.OrgResponseDTO;
import com.project.apirental.modules.organization.dto.OrgUpdateDTO;
import com.project.apirental.modules.organization.services.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
@Tag(name = "Organization Management", description = "Endpoints pour la gestion des organisations")
@SecurityRequirement(name = "bearerAuth") // Indique à Swagger que ces routes nécessitent un token
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "Obtenir les détails d'une organisation")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')") // Protection par Rôle
    public Mono<ResponseEntity<OrgResponseDTO>> getOrganization(@PathVariable UUID id) {
        return organizationService.getOrganization(id)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour une organisation")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')") // Protection par Rôle
    public Mono<ResponseEntity<OrgResponseDTO>> updateOrganization(
            @PathVariable UUID id,
            @RequestBody OrgUpdateDTO request) {
        return organizationService.updateOrganization(id, request)
                .map(ResponseEntity::ok);
    }
}
