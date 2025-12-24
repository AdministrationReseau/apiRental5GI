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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return authService.login(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/register/client")
    public Mono<ResponseEntity<UserEntity>> registerClient(@RequestBody RegisterRequest request) {
        return authService.registerClient(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/register/organization")
    public Mono<ResponseEntity<OrganizationEntity>> registerOrganization(@RequestBody OrgRegisterRequest request) {
        return authService.registerOrganization(request)
                .map(ResponseEntity::ok);
    }
}
