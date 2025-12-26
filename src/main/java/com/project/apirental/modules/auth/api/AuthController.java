package com.project.apirental.modules.auth.api;

import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.auth.dto.*;
import com.project.apirental.modules.auth.services.AuthService;
import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.organization.dto.OrgRegisterRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return authService.login(request).map(ResponseEntity::ok);
    }

    // Récupérer l'utilisateur connecté
    @GetMapping("/me")
    public Mono<ResponseEntity<UserEntity>> me() {
        return authService.getCurrentUser()
                .map(ResponseEntity::ok);
    }

    // Rafraîchir le token
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return authService.refreshToken(authHeader)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/register/client")
    public Mono<ResponseEntity<UserEntity>> registerClient(@RequestBody RegisterRequest request) {
        return authService.registerClient(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/register/organizationOwner")
    public Mono<ResponseEntity<OrganizationEntity>> registerOrganization(@RequestBody OrgRegisterRequest request) {
        return authService.registerOrganization(request)
                .map(ResponseEntity::ok);
    }
}
