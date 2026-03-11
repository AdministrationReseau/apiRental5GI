package com.project.apirental.modules.auth.services;

import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.auth.dto.PasswordUpdateDTO;
import com.project.apirental.modules.auth.dto.UserProfileUpdateDTO;
import com.project.apirental.modules.auth.repository.UserRepository;
import com.project.apirental.modules.permission.domain.PermissionEntity;
import com.project.apirental.modules.permission.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionRepository permissionRepository;

    @Transactional
    public Mono<UserEntity> updateProfile(UUID userId, UserProfileUpdateDTO dto) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé")))
            .flatMap(user -> {
                if (dto.firstname() != null) user.setFirstname(dto.firstname());
                if (dto.lastname() != null) user.setLastname(dto.lastname());
                user.setFullname(user.getFirstname() + " " + user.getLastname());
                return userRepository.save(user);
            });
    }

    @Transactional
    public Mono<Void> updatePassword(UUID userId, PasswordUpdateDTO dto) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé")))
            .flatMap(user -> {
                if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
                    return Mono.error(new RuntimeException("L'ancien mot de passe est incorrect"));
                }
                user.setPassword(passwordEncoder.encode(dto.newPassword()));
                return userRepository.save(user).then();
            });
    }

    public Flux<PermissionEntity> getUserPermissions(UUID userId) {
        return userRepository.findById(userId)
            .flatMapMany(user -> {
                if ("ADMIN".equals(user.getRole()) || "ORGANIZATION".equals(user.getRole())) {
                    return permissionRepository.findAll(); // Ils ont tous les droits
                }
                if (user.getPosteId() != null) {
                    return permissionRepository.findByPosteId(user.getPosteId());
                }
                return Flux.empty();
            });
    }
}
