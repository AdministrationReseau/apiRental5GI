package com.project.apirental.modules.rental.repository;

import com.project.apirental.modules.rental.domain.PaymentEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

public interface PaymentRepository extends R2dbcRepository<PaymentEntity, UUID> {
    Flux<PaymentEntity> findAllByRentalId(UUID rentalId);
}
