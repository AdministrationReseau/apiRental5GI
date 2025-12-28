package com.project.apirental.modules.poste.services;

import com.project.apirental.modules.permission.repository.PermissionRepository;
import com.project.apirental.modules.poste.domain.PosteEntity;
import com.project.apirental.modules.poste.dto.PosteRequestDTO;
import com.project.apirental.modules.poste.dto.PosteResponseDTO;
import com.project.apirental.modules.poste.repository.PosteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PosteService {

    private final PosteRepository posteRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public Mono<PosteResponseDTO> createPoste(UUID orgId, PosteRequestDTO request) {
        PosteEntity poste = PosteEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .name(request.name())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .isNewRecord(true)
                .build();

        return posteRepository.save(poste)
                .flatMap(savedPoste -> {
                    // Sauvegarde des liaisons permissions
                    if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
                        return Flux.fromIterable(request.permissionIds())
                                .flatMap(permId -> posteRepository.addPermission(savedPoste.getId(), permId))
                                .then(Mono.just(savedPoste));
                    }
                    return Mono.just(savedPoste);
                })
                .flatMap(this::enrichWithPermissions);
    }

    public Flux<PosteResponseDTO> getAvailablePostes(UUID orgId) {
        // On appelle la nouvelle requête qui inclut les postes systèmes
        return posteRepository.findAllByOrganizationIdOrSystem(orgId)
                .flatMap(this::enrichWithPermissions);
    }

    @Transactional
    public Mono<PosteResponseDTO> updatePoste(UUID posteId, PosteRequestDTO request) {
        return posteRepository.findById(posteId)
                .flatMap(poste -> {
                    if (request.name() != null) poste.setName(request.name());
                    if (request.description() != null) poste.setDescription(request.description());

                    return posteRepository.save(poste)
                            .flatMap(saved -> {
                                // Mise à jour des permissions : On supprime tout et on remet
                                if (request.permissionIds() != null) {
                                    return posteRepository.removeAllPermissions(saved.getId())
                                            .thenMany(Flux.fromIterable(request.permissionIds()))
                                            .flatMap(permId -> posteRepository.addPermission(saved.getId(), permId))
                                            .then(Mono.just(saved));
                                }
                                return Mono.just(saved);
                            });
                })
                .flatMap(this::enrichWithPermissions);
    }

    // Helper pour construire le DTO complet avec les objets Permission
    private Mono<PosteResponseDTO> enrichWithPermissions(PosteEntity poste) {
        return posteRepository.findPermissionIdsByPosteId(poste.getId())
                .flatMap(permissionRepository::findById)
                .collectList()
                .map(perms -> new PosteResponseDTO(
                        poste.getId(),
                        poste.getName(),
                        poste.getDescription(),
                        perms
                ));
    }
    /**
     * Récupère un poste par son ID et l'enrichit avec ses permissions.
     * Utilisé pour l'affichage détaillé et l'enrichissement du module Staff.
     */
    public Mono<PosteResponseDTO> getPosteById(UUID id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("L'ID du poste ne peut pas être nul"));
        }
        
        return posteRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Poste non trouvé avec l'ID : " + id)))
                .flatMap(this::enrichWithPermissions);
    }
}
