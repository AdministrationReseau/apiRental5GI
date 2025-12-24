package com.project.apirental.modules.auth.repository;

import java.util.UUID;

import com.project.apirental.modules.auth.domain.UserEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<UserEntity, UUID> {
    Mono<UserEntity> findByEmail(String email);
}
