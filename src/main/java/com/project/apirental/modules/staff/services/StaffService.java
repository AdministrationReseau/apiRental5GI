package com.project.apirental.modules.staff.services;

import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.auth.repository.UserRepository;
import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.organization.services.OrganizationService;
import com.project.apirental.modules.poste.services.PosteService;
import com.project.apirental.modules.staff.domain.StaffEntity;
import com.project.apirental.modules.staff.dto.StaffRequestDTO;
import com.project.apirental.modules.staff.dto.StaffResponseDTO;
import com.project.apirental.modules.staff.dto.StaffUpdateDTO;
import com.project.apirental.modules.staff.mapper.StaffMapper;
import com.project.apirental.modules.staff.repository.StaffRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.shared.events.AuditEvent;

// import com.project.apirental.shared.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
// import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository   planRepository;
    private final PosteService posteService;
    private final StaffMapper staffMapper;
    private final ApplicationEventPublisher eventPublisher;
    // private final ApplicationEventPublisher eventPublisher;

    @Transactional
public Mono<StaffResponseDTO> addStaffToOrganization(UUID orgId, StaffRequestDTO request) {
    return userRepository.findByEmail(request.userEmail())
        .switchIfEmpty(Mono.<UserEntity>error(new RuntimeException("Utilisateur non trouvé")))
        .flatMap(user -> 
            organizationRepository.findById(Objects.requireNonNull(orgId))
                .switchIfEmpty(Mono.<OrganizationEntity>error(new RuntimeException("Organisation non trouvée")))
                .flatMap(org -> 
                    planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .flatMap(plan -> 
                            staffRepository.findByUserIdAndOrganizationId(user.getId(), orgId)
                                // ICI : On précise <StaffEntity> pour que le compilateur sache quoi attendre
                                .flatMap(existing -> Mono.<StaffEntity>error(new RuntimeException("Déjà membre du personnel")))
                                .switchIfEmpty(Mono.defer(() -> {
                                    
                                    // ICI : On précise aussi pour le quota
                                    if (org.getCurrentUsers() >= plan.getMaxUsers()) {
                                        return Mono.<StaffEntity>error(new RuntimeException("Quota atteint"));
                                    }

                                    StaffEntity staff = StaffEntity.builder()
                                            .id(UUID.randomUUID())
                                            .userId(user.getId())
                                            .organizationId(orgId)
                                            .agencyId(request.agencyId())
                                            .posteId(request.posteId())
                                            .status("ACTIVE")
                                            .hiredAt(LocalDateTime.now())
                                            .isNewRecord(true)
                                            .build();

                                    return staffRepository.save(staff);
                                }))
                        )
                )
                // Une fois que tout le bloc interne a renvoyé un StaffEntity, on l'enrichit
                .flatMap(savedStaff -> 
                    organizationService.updateStaffCounter(orgId, 1) // On incrémente l'org
                        .then(updateAgencyStaffCounter(request.agencyId(), 1)) // On incrémente l'agence
                        .then(enrichStaff(savedStaff)) // On transforme en DTO
                )
        );
}

    /**
     * Helper pour mettre à jour le compteur spécifique de l'agence
     */
    private Mono<Void> updateAgencyStaffCounter(UUID agencyId, int increment) {
        return agencyRepository.findById(Objects.requireNonNull(agencyId))
                .flatMap(agency -> {
                    agency.setTotalPersonnel(agency.getTotalPersonnel() + increment);
                    return agencyRepository.save(agency);
                }).then();
    }

    // READ : Liste par Agence

    public Flux<StaffResponseDTO> getStaffByAgency(UUID agencyId) {
        return staffRepository.findAllByAgencyId(agencyId)
                .flatMap(this::enrichStaff);
    }

    // READ : Par ID
    public Mono<StaffResponseDTO> getStaffById(UUID id) {
        return staffRepository.findById(id)
                .flatMap(this::enrichStaff)
                .switchIfEmpty(Mono.error(new RuntimeException("Staff non trouvé")));
    }

    // READ : Liste par Organisation
    public Flux<StaffResponseDTO> getStaffByOrganization(UUID orgId) {
        return staffRepository.findAllByOrganizationId(orgId)
                .flatMap(this::enrichStaff);
    }

    // UPDATE : Changer de poste, d'agence ou de statut
    @Transactional
    public Mono<StaffResponseDTO> updateStaff(UUID staffId, StaffUpdateDTO request) {
        return staffRepository.findById(staffId)
                .flatMap(staff -> {
                    UUID oldAgencyId = staff.getAgencyId();
                    UUID newAgencyId = request.agencyId();

                    // Si on change d'agence, on gère les compteurs
                    Mono<Void> counterUpdate = Mono.empty();
                    if (newAgencyId != null && !newAgencyId.equals(oldAgencyId)) {
                        staff.setAgencyId(newAgencyId);
                        counterUpdate = updateAgencyCounter(oldAgencyId, -1)
                                .then(updateAgencyCounter(newAgencyId, 1));
                    }

                    if (request.posteId() != null)
                        staff.setPosteId(request.posteId());
                    if (request.status() != null)
                        staff.setStatus(request.status());

                    return counterUpdate
                            .then(staffRepository.save(staff))
                            .flatMap(this::enrichStaff);
                })
                .doOnSuccess(s -> eventPublisher
                        .publishEvent(new AuditEvent("UPDATE_STAFF", "STAFF", "Staff mis à jour ID: " + staffId)));
    }

    // DELETE : Supprimer un membre du staff
    @Transactional
    public Mono<Void> deleteStaff(UUID staffId) {
        return staffRepository.findById(staffId)
                .flatMap(staff -> staffRepository.delete(staff)
                        .then(organizationService.updateStaffCounter(staff.getOrganizationId(), -1))
                        .then(updateAgencyCounter(staff.getAgencyId(), -1)))
                .doOnSuccess(v -> eventPublisher
                        .publishEvent(new AuditEvent("DELETE_STAFF", "STAFF", "Staff supprimé ID: " + staffId)));
    }

    // HELPERS
    private Mono<Void> updateAgencyCounter(UUID agencyId, int increment) {
        return agencyRepository.findById(agencyId)
                .flatMap(agency -> {
                    agency.setTotalPersonnel(agency.getTotalPersonnel() + increment);
                    return agencyRepository.save(agency);
                }).then();
    }

    private Mono<StaffResponseDTO> enrichStaff(StaffEntity staff) {
        return Mono.zip(
                userRepository.findById(staff.getUserId()),
                posteService.getPosteById(staff.getPosteId()))
                .map(tuple -> staffMapper.toDto(staff, tuple.getT1(), tuple.getT2()));
    }

}