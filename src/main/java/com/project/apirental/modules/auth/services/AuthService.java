package com.project.apirental.modules.auth.services;

import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.auth.dto.*;
import com.project.apirental.modules.organization.dto.OrgRegisterRequest;
import com.project.apirental.modules.auth.repository.UserRepository;
import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.subscription.services.SubscriptionService;
import com.project.apirental.shared.events.AuditEvent;
import com.project.apirental.shared.security.JwtUtil;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository orgRepository;
    
    private final SubscriptionService subscriptionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    // Authentification (Login)
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .map(u -> {
                    // Audit de succès
                    eventPublisher.publishEvent(new AuditEvent("LOGIN", "AUTH", "User logged in: " + u.getEmail()));
                    return new AuthResponse(jwtUtil.generateToken(u.getEmail(), u.getRole()));
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Bad credentials")));
    }

    // Nouvelle méthode : Récupérer l'utilisateur courant via le contexte de sécurité
    public Mono<UserEntity> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userRepository::findByEmail);
    }

    // Nouvelle méthode : Refresh Token
    public Mono<AuthResponse> refreshToken(String oldToken) {
        if (oldToken.startsWith("Bearer ")) {
            oldToken = oldToken.substring(7);
        }

        if (jwtUtil.validateToken(oldToken)) {
            String email = jwtUtil.getUsernameFromToken(oldToken);
            return userRepository.findByEmail(email)
                    .map(user -> new AuthResponse(jwtUtil.generateToken(user.getEmail(), user.getRole())));
        }
        return Mono.error(new RuntimeException("Invalid Token"));
    }

    // Inscription Client Simple
    public Mono<UserEntity> registerClient(RegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.error(new RuntimeException("Email already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    UserEntity user = UserEntity.builder()
                            .id(UUID.randomUUID())
                            .firstname(request.firstname())
                            .lastname(request.lastname())
                            .fullname(request.firstname() +" "+ request.lastname())
                            .email(request.email())
                            .password(passwordEncoder.encode(request.password()))
                            .role("CLIENT")
                            .isNewRecord(true) // <--- IMPORTANT : Force l'INSERT
                            .build();
                    return userRepository.save(user)
                            .doOnSuccess(u -> eventPublisher.publishEvent(new AuditEvent("REGISTER_CLIENT", "AUTH", "New client: " + u.getEmail())));
                })).cast(UserEntity.class);
    }

    // Scénario Organisation: Création User + Création Org
    @Transactional // Transaction réactive
    public Mono<OrganizationEntity> registerOrganization(OrgRegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.error(new RuntimeException("Email already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    // 1. Créer le User
                    UserEntity user = UserEntity.builder()
                            .id(UUID.randomUUID())
                            .firstname(request.firstname())
                            .lastname(request.lastname())
                            .fullname(request.firstname() +" "+ request.lastname())
                            .email(request.email())
                            .password(passwordEncoder.encode(request.password()))
                            .role("ORGANIZATION")
                            .isNewRecord(true) // <--- IMPORTANT : Force l'INSERT
                            .build();

                    return userRepository.save(user).flatMap(savedUser -> {
                        // 2. Créer l'Organisation liée
                        OrganizationEntity org = OrganizationEntity.builder()
                                .id(UUID.randomUUID())
                                .name(request.orgName())
                                .ownerId(savedUser.getId())
                                .isNewRecord(true) // <--- IMPORTANT : Force l'INSERT
                                .build();
                        return orgRepository.save(org)
                                .flatMap(savedOrg -> 
                                    // 3. Créer la souscription FREE (Chaînage réactif)
                                    subscriptionService.initializeDefaultSubscription(savedOrg.getId())
                                        .thenReturn(savedOrg) // On retourne l'organisation pour le flux final
                                )
                                .doOnSuccess(o -> eventPublisher.publishEvent(new AuditEvent("REGISTER_ORG", "AUTH", "New Org: " + o.getName())));
                    });
                })).cast(OrganizationEntity.class);
    }
}
