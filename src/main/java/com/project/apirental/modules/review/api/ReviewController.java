package com.project.apirental.modules.review.api;

import com.project.apirental.modules.review.dto.ReviewRequestDTO;
import com.project.apirental.modules.review.dto.ReviewResponseDTO;
import com.project.apirental.modules.review.services.ReviewService;
import com.project.apirental.shared.enums.ResourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Management", description = "Gestion des avis sur les véhicules et chauffeurs")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Ajouter un avis")
    @PostMapping
    public Mono<ResponseEntity<ReviewResponseDTO>> addReview(@RequestBody @Valid ReviewRequestDTO request) {
        return reviewService.addReview(request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les avis d'une ressource")
    @GetMapping("/{type}/{id}")
    public Flux<ReviewResponseDTO> getReviews(
            @PathVariable ResourceType type,
            @PathVariable UUID id) {
        return reviewService.getReviews(type, id);
    }
}
