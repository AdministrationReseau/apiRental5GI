package com.project.apirental.modules.rental.api;

import com.project.apirental.modules.rental.domain.RentalEntity;
import com.project.apirental.modules.rental.dto.PaymentRequest;
import com.project.apirental.modules.rental.dto.RentalDetailResponseDTO;
import com.project.apirental.modules.rental.dto.RentalInitRequest;
import com.project.apirental.modules.rental.dto.RentalInitResponse;
import com.project.apirental.modules.rental.services.RentalPaymentService;
import com.project.apirental.modules.rental.services.RentalService;
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

    // =================================================================================
    // DÉTAILS (Client & Agence/Org)
    // =================================================================================

    @Operation(summary = "Obtenir les détails complets d'une réservation ou location")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<RentalDetailResponseDTO>> getRentalDetails(@PathVariable UUID id) {
        return rentalService.getRentalDetails(id).map(ResponseEntity::ok);
    }

    // =================================================================================
    // ACTIONS (Init, Pay, Start, End, Cancel)
    // =================================================================================

    @Operation(summary = "Initier une réservation (Devis + Vérification)")
    @PostMapping("/init")
    public Mono<ResponseEntity<RentalInitResponse>> initiateRental(@RequestBody @Valid RentalInitRequest request) {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(userRepository::findByEmail)
            .flatMap(user -> rentalService.initiateRental(user.getId(), request))
            .map(ResponseEntity::ok);
    }

    @Operation(summary = "Payer une réservation (60% ou solde)")
    @PostMapping("/{id}/pay")
    public Mono<ResponseEntity<RentalEntity>> payRental(
            @PathVariable UUID id,
            @RequestBody @Valid PaymentRequest request) {
        return paymentService.processPayment(id, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Démarrer la location (Récupération véhicule)")
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
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

    @Operation(summary = "Annuler une réservation (Client)")
    @PutMapping("/{id}/cancel")
    public Mono<ResponseEntity<RentalEntity>> cancelRental(@PathVariable UUID id) {
        return rentalService.cancelRental(id).map(ResponseEntity::ok);
    }

    // =================================================================================
    // LISTINGS CLIENT
    // =================================================================================

    @Operation(summary = "CLIENT: Mes réservations actives (En attente, Réservée, Payée)")
    @GetMapping("/client/reservations/active")
    public Flux<RentalEntity> getClientActiveReservations() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(userRepository::findByEmail)
            .flatMapMany(user -> rentalService.getClientActiveReservations(user.getId()));
    }

    @Operation(summary = "CLIENT: Mes locations (En cours et Terminées)")
    @GetMapping("/client/rentals/history")
    public Flux<RentalEntity> getClientRentalsHistory() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(userRepository::findByEmail)
            .flatMapMany(user -> rentalService.getClientRentalsHistory(user.getId()));
    }

    // =================================================================================
    // LISTINGS AGENCE
    // =================================================================================

    @Operation(summary = "AGENCE: Toutes les réservations (Actives et Annulées)")
    @GetMapping("/agency/{agencyId}/reservations")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Flux<RentalEntity> getAgencyReservations(@PathVariable UUID agencyId) {
        return rentalService.getAgencyReservations(agencyId);
    }

    @Operation(summary = "AGENCE: Toutes les locations (En cours et Terminées)")
    @GetMapping("/agency/{agencyId}/rentals")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Flux<RentalEntity> getAgencyRentals(@PathVariable UUID agencyId) {
        return rentalService.getAgencyRentals(agencyId);
    }

    // =================================================================================
    // LISTINGS ORGANISATION
    // =================================================================================

    @Operation(summary = "ORGANISATION: Toutes les réservations de toutes les agences")
    @GetMapping("/org/{orgId}/reservations")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<RentalEntity> getOrgReservations(@PathVariable UUID orgId) {
        return rentalService.getOrganizationReservations(orgId);
    }

    @Operation(summary = "ORGANISATION: Toutes les locations de toutes les agences")
    @GetMapping("/org/{orgId}/rentals")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<RentalEntity> getOrgRentals(@PathVariable UUID orgId) {
        return rentalService.getOrganizationRentals(orgId);
    }
}
