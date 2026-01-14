package com.project.apirental.modules.driver.api;

import com.project.apirental.modules.driver.dto.DriverResponseDTO;
import com.project.apirental.modules.driver.services.DriverService;
import com.project.apirental.modules.driver.dto.DriverDetailResponseDTO;
import com.project.apirental.modules.driver.dto.UpdateDriverStatusDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver Management", description = "Gestion des conducteurs et leurs documents")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService driverService;

    @Operation(summary = "Créer un conducteur avec fichiers (Profil, CNI, Permis)")
    @PostMapping(value = "/org/{orgId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<DriverResponseDTO>> create(
            @PathVariable UUID orgId,
            @Parameter(description = "ID de l'agence d'affectation", required = true)
            @RequestPart("agencyId") String agencyIdStr,

            @Parameter(required = true) @RequestPart("firstname") String firstname,
            @Parameter(required = true) @RequestPart("lastname") String lastname,
            @Parameter(required = true) @RequestPart("tel") String tel,
            @Parameter(required = true) @RequestPart("age") String ageStr,
            @Parameter(description = "0: Homme, 1: Femme", required = true)
            @RequestPart("gender") String genderStr,

            @Parameter(description = "Photo de profil", required = true)
            @RequestPart("profil") FilePart profilFile,

            @Parameter(description = "Scan de la CNI", required = true)
            @RequestPart("cni") FilePart cniFile,

            @Parameter(description = "Scan du permis de conduire", required = true)
            @RequestPart("license") FilePart licenseFile
    ) {
        // Conversion des types
        UUID agencyId = UUID.fromString(agencyIdStr);
        Integer age = Integer.parseInt(ageStr);
        Integer gender = Integer.parseInt(genderStr);

        return driverService.createDriver(
                orgId, agencyId, firstname, lastname, tel, age, gender,
                profilFile, cniFile, licenseFile
        ).map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les conducteurs d'une organisation")
    @GetMapping("/org/{orgId}")
    public Flux<DriverResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return driverService.getDriversByOrg(orgId);
    }

    @Operation(summary = "Lister les conducteurs d'une agence")
    @GetMapping("/agency/{agencyId}")
    public Flux<DriverResponseDTO> getByAgency(@PathVariable UUID agencyId) {
        return driverService.getDriversByAgency(agencyId);
    }

    @Operation(summary = "Détails d'un conducteur")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<DriverResponseDTO>> getById(@PathVariable UUID id) {
        return driverService.getDriverById(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtenir les détails complets (Planning + Prix) d'un conducteur")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<DriverDetailResponseDTO>> getDriverDetails(@PathVariable UUID id) {
        return driverService.getDriverDetails(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour statut, motif, planning et prix")
    @PatchMapping("/{id}/status-pricing")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<DriverDetailResponseDTO>> updateStatusAndPricing(
            @PathVariable UUID id,
            @RequestBody UpdateDriverStatusDTO request) {
        return driverService.updateDriverStatusAndPricing(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Changer l'agence d'un conducteur")
    @PutMapping("/{id}/change-agency")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<DriverResponseDTO>> changeAgency(
            @PathVariable UUID id,
            @RequestParam UUID newAgencyId) {
        return driverService.changeAgency(id, newAgencyId).map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer un conducteur")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return driverService.deleteDriver(id).then(Mono.just(ResponseEntity.noContent().build()));
    }
}
