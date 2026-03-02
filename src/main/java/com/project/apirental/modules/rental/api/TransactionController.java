package com.project.apirental.modules.rental.api;

import com.project.apirental.modules.auth.repository.UserRepository;
import com.project.apirental.modules.rental.dto.TransactionResponseDTO;
import com.project.apirental.modules.rental.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Financial Transactions", description = "Historique des paiements et abonnements")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @Operation(summary = "CLIENT: Mes transactions (Historique paiements)")
    @GetMapping("/client/history")
    public Flux<TransactionResponseDTO> getMyTransactions() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(userRepository::findByEmail)
            .flatMapMany(user -> transactionService.getClientTransactions(user.getId()));
    }

    @Operation(summary = "AGENCE: Historique des transactions (Revenus)")
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('AGENT')")
    public Flux<TransactionResponseDTO> getAgencyTransactions(@PathVariable UUID agencyId) {
        return transactionService.getAgencyTransactions(agencyId);
    }

    @Operation(summary = "ORGANISATION: Grand livre (Revenus Agences + Coûts Abonnements)")
    @GetMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<TransactionResponseDTO> getOrganizationTransactions(@PathVariable UUID orgId) {
        return transactionService.getOrganizationTransactions(orgId);
    }
}
