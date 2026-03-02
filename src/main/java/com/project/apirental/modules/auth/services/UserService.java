package com.project.apirental.modules.auth.services;

import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.auth.dto.PasswordUpdateDTO;
import com.project.apirental.modules.auth.dto.UserProfileUpdateDTO;
import com.project.apirental.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Mono<UserEntity> updateProfile(UUID userId, UserProfileUpdateDTO dto) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé")))
            .flatMap(user -> {
                if (dto.firstname() != null) user.setFirstname(dto.firstname());
                if (dto.lastname() != null) user.setLastname(dto.lastname());

                // Recalcul du nom complet
                user.setFullname(user.getFirstname() + " " + user.getLastname());

                return userRepository.save(user);
            });
    }

    @Transactional
    public Mono<Void> updatePassword(UUID userId, PasswordUpdateDTO dto) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé")))
            .flatMap(user -> {
                // Vérification de l'ancien mot de passe
                if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
                    return Mono.error(new RuntimeException("L'ancien mot de passe est incorrect"));
                }

                // Encodage et sauvegarde du nouveau mot de passe
                user.setPassword(passwordEncoder.encode(dto.newPassword()));
                return userRepository.save(user).then();
            });
    }
}
