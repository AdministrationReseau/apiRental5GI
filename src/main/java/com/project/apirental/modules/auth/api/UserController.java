package com.project.apirental.modules.auth.api;

import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.auth.dto.PasswordUpdateDTO;
import com.project.apirental.modules.auth.dto.UserProfileUpdateDTO;
import com.project.apirental.modules.auth.services.AuthService;
import com.project.apirental.modules.auth.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Gestion du profil utilisateur et sécurité")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @Operation(summary = "Mettre à jour le profil de l'utilisateur connecté")
    @PutMapping("/profile")
    public Mono<ResponseEntity<UserEntity>> updateProfile(@RequestBody @Valid UserProfileUpdateDTO dto) {
        return authService.getCurrentUser()
                .flatMap(user -> userService.updateProfile(user.getId(), dto))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Changer le mot de passe de l'utilisateur connecté")
    @PutMapping("/password")
    public Mono<ResponseEntity<String>> updatePassword(@RequestBody @Valid PasswordUpdateDTO dto) {
        return authService.getCurrentUser()
                .flatMap(user -> userService.updatePassword(user.getId(), dto))
                .then(Mono.just(ResponseEntity.ok("Mot de passe mis à jour avec succès")));
    }
}
