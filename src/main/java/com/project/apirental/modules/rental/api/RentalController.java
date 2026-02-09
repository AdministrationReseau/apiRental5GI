package com.project.apirental.modules.rental.api;

import com.project.apirental.modules.rental.domain.RentalEntity;
import com.project.apirental.modules.rental.dto.AgencyRentalRequest;
import com.project.apirental.modules.rental.dto.PaymentRequest;
import com.project.apirental.modules.rental.dto.RentalInitRequest;
import com.project.apirental.modules.rental.dto.RentalInitResponse;
import com.project.apirental.modules.rental.services.RentalPaymentService;
import com.project.apirental.modules.rental.services.RentalService;
import com.project.apirental.shared.enums.RentalStatus;
import com.project.apirental.modules.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Tag(name = "Rental Process", description = "Gestion des locations (Réservation, Paiement, Cycle de vie)")
@SecurityRequirement(name = "bearerAuth")
public class RentalController {

    private final RentalService rentalService;
    private final RentalPaymentService paymentService;
    private final UserRepository userRepository;

    @Operation(summary = "Initier une location (Vérification et Devis)")
    @PostMapping("/init")
    public Mono<ResponseEntity<RentalInitResponse>> initiateRental(@RequestBody @Valid RentalInitRequest request) {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(userRepository::findByEmail)
            .flatMap(user -> rentalService.initiateRental(user.getId(), request))
            .map(ResponseEntity::ok);
    }

    @Operation(summary = "Effectuer un paiement pour une location")
    @PostMapping("/{id}/pay")
    public Mono<ResponseEntity<RentalEntity>> payRental(
            @PathVariable UUID id,
            @RequestBody @Valid PaymentRequest request) {
        return paymentService.processPayment(id, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Démarrer la location (Récupération véhicule)")
    @PutMapping("/{id}/start")
    public Mono<ResponseEntity<RentalEntity>> startRental(@PathVariable UUID id) {
        return rentalService.startRental(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Signaler la fin de la location (Client)")
    @PutMapping("/{id}/end-signal")
    public Mono<ResponseEntity<RentalEntity>> signalEnd(@PathVariable UUID id) {
        return rentalService.signalEndRental(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Valider le retour du véhicule (Agence)")
    @PutMapping("/{id}/validate-return")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Mono<ResponseEntity<RentalEntity>> validateReturn(@PathVariable UUID id) {
        return rentalService.validateReturn(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les locations d'une agence (Filtre optionnel par statut)")
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Flux<RentalEntity> getByAgency(
            @PathVariable UUID agencyId,
            @RequestParam(required = false) RentalStatus status) {
        if (status != null) {
            return rentalService.getRentalsByAgencyAndStatus(agencyId, status);
        }
        return rentalService.getRentalsByAgency(agencyId);
    }

    @Operation(summary = "Créer une location par l'agence (Client Walk-in)")
    @PostMapping("/agency/{agencyId}/create")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Mono<ResponseEntity<RentalInitResponse>> createAgencyRental(
            @PathVariable UUID agencyId,
            @RequestBody @Valid AgencyRentalRequest request) {
        return rentalService.createAgencyRental(agencyId, request)
                .map(ResponseEntity::ok);
    }
}
