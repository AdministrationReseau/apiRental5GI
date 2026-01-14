package com.project.apirental.modules.schedule.services;

import com.project.apirental.modules.schedule.domain.ScheduleEntity;
import com.project.apirental.modules.schedule.repository.ScheduleRepository;
import com.project.apirental.shared.dto.ScheduleRequestDTO;
import com.project.apirental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;

    /**
     * Ajoute une période d'indisponibilité (Panne, Maladie, etc.)
     */
    @Transactional
    public Mono<ScheduleEntity> addUnavailability(UUID orgId, ResourceType type, UUID resourceId, ScheduleRequestDTO request) {
        if (request.endDate().isBefore(request.startDate())) {
            return Mono.error(new IllegalArgumentException("La date de fin doit être après la date de début"));
        }
        if (request.startDate().isBefore(LocalDateTime.now().minusMinutes(1))) {
           // Sauf si c'est pour régulariser. Ici on bloque le passé.
           return Mono.error(new IllegalArgumentException("Impossible de planifier dans le passé"));
        }

        ScheduleEntity schedule = ScheduleEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .resourceType(type)
                .resourceId(resourceId)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(request.status().toUpperCase())
                .reason(request.reason())
                .createdAt(LocalDateTime.now())
                .isNewRecord(true)
                .build();

        return scheduleRepository.save(schedule);
    }

    public Flux<ScheduleEntity> getResourceSchedule(ResourceType type, UUID resourceId) {
        // On ne retourne que les événements futurs ou en cours
        return scheduleRepository.findFutureSchedules(type, resourceId, LocalDateTime.now());
    }
}
