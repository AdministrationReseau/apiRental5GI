package com.project.apirental.modules.poste.api;

import com.project.apirental.modules.poste.dto.PosteRequestDTO;
import com.project.apirental.modules.poste.dto.PosteResponseDTO;
import com.project.apirental.modules.poste.services.PosteService;
import com.project.apirental.modules.poste.repository.PosteRepository;
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
@RequestMapping("/api/postes")
@RequiredArgsConstructor
@Tag(name = "Poste & Roles", description = "Gestion des postes et permissions employés")
@SecurityRequirement(name = "bearerAuth")
public class PosteController {

    private final PosteService posteService;
    private final PosteRepository PosteRepository;

    @Operation(summary = "Lister les postes d'une organisation")
    @GetMapping("/org/{orgId}/postes")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<PosteResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return posteService.getAvailablePostes(orgId);
    }

    @Operation(summary = "Créer un nouveau poste avec permissions")
    @PostMapping("/org/{orgId}/poste")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<PosteResponseDTO>> create(@PathVariable UUID orgId, @RequestBody PosteRequestDTO request) {
        return posteService.createPoste(orgId, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour un poste")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<PosteResponseDTO>> update(@PathVariable UUID id, @RequestBody PosteRequestDTO request) {
        return PosteRepository.findById(id)
                .flatMap(poste -> {
                    // Si l'organizationId est NULL, c'est un poste système : INTERDIT de modifier
                    if (poste.getOrganizationId() == null) {
                        return Mono.error(new RuntimeException("Impossible de modifier un poste par défaut du système"));
                    }
                    return posteService.updatePoste(id, request);
                })
                .map(ResponseEntity::ok);
    }
}
