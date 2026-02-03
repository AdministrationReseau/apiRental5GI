package com.project.apirental.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.apirental.modules.agency.domain.AgencyEntity;
import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.auth.domain.UserEntity;
import com.project.apirental.modules.auth.repository.UserRepository;
import com.project.apirental.modules.driver.domain.DriverEntity;
import com.project.apirental.modules.driver.repository.DriverRepository;
import com.project.apirental.modules.organization.domain.OrganizationEntity;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.poste.domain.PosteEntity;
import com.project.apirental.modules.poste.repository.PosteRepository;
import com.project.apirental.modules.staff.repository.StaffRepository;
import com.project.apirental.modules.subscription.domain.SubscriptionEntity;
import com.project.apirental.modules.subscription.domain.SubscriptionPlanEntity;
import com.project.apirental.modules.subscription.repository.SubscriptionPlanRepository;
import com.project.apirental.modules.subscription.repository.SubscriptionRepository;
import com.project.apirental.modules.vehicle.domain.VehicleCategoryEntity;
import com.project.apirental.modules.vehicle.domain.VehicleEntity;
import com.project.apirental.modules.vehicle.repository.CategoryRepository;
import com.project.apirental.modules.vehicle.repository.VehicleRepository;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AgencyRepository agencyRepository;
    private final PosteRepository posteRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CategoryRepository categoryRepository;
    private final StaffRepository staffRepository;

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        log.info("🌱 Démarrage du Seeder de données complet...");

        seedOrganizations()
            .doOnError(error -> log.error("❌ Erreur critique lors du seeding : ", error))
            .doOnTerminate(() -> log.info("🏁 Processus de seeding terminé."))
            .subscribe();
    }

    private Mono<Void> seedOrganizations() {
        return createFullOrganization("org.free1@demo.com", "Free Corp 1", "FREE")
            .then(createFullOrganization("org.free2@demo.com", "Free Corp 2", "FREE"))
            .then(createFullOrganization("org.pro1@demo.com", "Pro Solutions 1", "PRO"))
            .then(createFullOrganization("org.pro2@demo.com", "Pro Solutions 2", "PRO"));
    }

    private Mono<Void> createFullOrganization(String email, String orgName, String planName) {
        return userRepository.findByEmail(email)
            .flatMap(existing -> {
                log.info("⚠️ L'organisation {} existe déjà.", orgName);
                return Mono.just(existing);
            })
            .switchIfEmpty(Mono.defer(() ->
                planRepository.findByName(planName)
                    .switchIfEmpty(Mono.error(new RuntimeException("Plan " + planName + " introuvable")))
                    .flatMap(plan -> {
                        log.info("🏗️ Création de l'organisation : {} (Plan: {})", orgName, planName);

                        UserEntity owner = UserEntity.builder()
                            .id(UUID.randomUUID())
                            .firstname("Admin")
                            .lastname(orgName)
                            .fullname("Admin " + orgName)
                            .email(email)
                            .password(passwordEncoder.encode("pass1234"))
                            .role("ORGANIZATION")
                            .status("ACTIVE")
                            .isNewRecord(true)
                            .build();

                        return userRepository.save(owner).flatMap(savedOwner -> {
                            savedOwner.setNewRecord(false);

                            LocalDateTime expiresAt = (plan.getDurationDays() > 0)
                                ? LocalDateTime.now().plusDays(plan.getDurationDays()) : null;

                            OrganizationEntity org = OrganizationEntity.builder()
                                .id(UUID.randomUUID())
                                .name(orgName)
                                .description("Organisation de démonstration")
                                .ownerId(savedOwner.getId())
                                .email(email)
                                .phone("+237600000000")
                                .address("Quartier Général")
                                .city("Douala")
                                .country("CM")
                                .subscriptionPlanId(plan.getId())
                                .subscriptionExpiresAt(expiresAt)
                                .isVerified(true)
                                .logoUrl("https://placehold.co/200x200/png?text=Logo+" + orgName.replaceAll(" ", "+"))
                                .registrationNumber("REG-" + System.currentTimeMillis())
                                .taxNumber("TAX-" + System.currentTimeMillis())
                                .isNewRecord(true)
                                .build();

                            return organizationRepository.save(org).flatMap(savedOrg -> {
                                savedOwner.setOrganizationId(savedOrg.getId());

                                return subscriptionRepository.save(SubscriptionEntity.builder()
                                        .id(UUID.randomUUID())
                                        .organizationId(savedOrg.getId())
                                        .planType(planName)
                                        .status("ACTIVE")
                                        .startDate(LocalDateTime.now())
                                        .endDate(expiresAt)
                                        .isNewRecord(true).build())
                                    .then(userRepository.save(savedOwner))
                                    .then(seedPostes(savedOrg))
                                    .flatMap(postes ->
                                        seedAgenciesAndResources(savedOrg, plan, postes)
                                        // CORRECTION ICI : On retourne l'objet UserEntity pour satisfaire le type du switchIfEmpty
                                        .thenReturn(savedOwner)
                                    );
                            });
                        });
                    })
            ))
            .then(); // On convertit le résultat final (UserEntity ou Void) en Mono<Void> pour la méthode
    }

    private Mono<Map<String, PosteEntity>> seedPostes(OrganizationEntity org) {
        PosteEntity managerPoste = PosteEntity.builder().id(UUID.randomUUID()).organizationId(org.getId()).name("Manager").description("Gestionnaire d'agence").isNewRecord(true).build();
        PosteEntity agentPoste = PosteEntity.builder().id(UUID.randomUUID()).organizationId(org.getId()).name("Agent").description("Agent de comptoir").isNewRecord(true).build();
        PosteEntity mecanoPoste = PosteEntity.builder().id(UUID.randomUUID()).organizationId(org.getId()).name("Mécanicien").description("Maintenance").isNewRecord(true).build();

        return Flux.just(managerPoste, agentPoste, mecanoPoste)
            .flatMap(posteRepository::save)
            .collectMap(PosteEntity::getName);
    }

    private Mono<Void> seedAgenciesAndResources(OrganizationEntity org, SubscriptionPlanEntity plan, Map<String, PosteEntity> postes) {
        int agencyCount = "FREE".equals(plan.getName()) ? 1 : 3;

        int vehiclesPerAgency = Math.max(1, plan.getMaxVehicles() / agencyCount);
        int driversPerAgency = Math.max(1, plan.getMaxDrivers() / agencyCount);
        int staffPerAgency = "FREE".equals(plan.getName()) ? 2 : 5;

        return Flux.range(1, agencyCount)
            .flatMap(i -> {
                String agencyName = org.getName() + " - Agence " + i;

                AgencyEntity agency = AgencyEntity.builder()
                    .id(UUID.randomUUID())
                    .organizationId(org.getId())
                    .name(agencyName)
                    .address("Adresse Agence " + i)
                    .city("Douala")
                    .email("agence" + i + "." + org.getId().toString().substring(0,4) + "@demo.com")
                    .phone("69900000" + i)
                    .is24Hours(true)
                    .logoUrl("https://placehold.co/150x150?text=Agence+" + i)
                    .isNewRecord(true)
                    .build();

                return agencyRepository.save(agency).flatMap(savedAgency -> {
                    savedAgency.setNewRecord(false);

                    return seedStaff(org, savedAgency, staffPerAgency, postes)
                        .flatMap(managerId -> {
                            savedAgency.setManagerId(managerId);
                            savedAgency.setTotalPersonnel(staffPerAgency);
                            return agencyRepository.save(savedAgency);
                        })
                        .then(seedVehicles(org, savedAgency, vehiclesPerAgency))
                        .flatMap(vehicleCount -> {
                            savedAgency.setTotalVehicles(vehicleCount);
                            savedAgency.setActiveVehicles(vehicleCount);
                            return agencyRepository.save(savedAgency);
                        })
                        .then(seedDrivers(org, savedAgency, driversPerAgency))
                        .flatMap(driverCount -> {
                            savedAgency.setTotalDrivers(driverCount);
                            savedAgency.setActiveDrivers(driverCount);
                            return agencyRepository.save(savedAgency);
                        });
                });
            })
            .then(updateOrganizationCounters(org.getId(), agencyCount, plan));
    }

    private Mono<UUID> seedStaff(OrganizationEntity org, AgencyEntity agency, int count, Map<String, PosteEntity> postes) {
        return Flux.range(1, count)
            .flatMap(i -> {
                boolean isManager = (i == 1);
                String roleName = isManager ? "Manager" : (i % 2 == 0 ? "Agent" : "Mécanicien");
                PosteEntity poste = postes.getOrDefault(roleName, postes.get("Agent"));

                String email = "staff." + agency.getId().toString().substring(0,4) + "." + i + "@demo.com";

                UserEntity staff = UserEntity.builder()
                    .id(UUID.randomUUID())
                    .organizationId(org.getId())
                    .agencyId(agency.getId())
                    .posteId(poste.getId())
                    .firstname(isManager ? "Manager" : "Employé")
                    .lastname(agency.getName() + " " + i)
                    .fullname((isManager ? "Manager " : "Employé ") + i)
                    .email(email)
                    .password(passwordEncoder.encode("pass1234"))
                    .role("STAFF")
                    .status("ACTIVE")
                    .hiredAt(LocalDateTime.now())
                    .isNewRecord(true)
                    .build();

                return userRepository.save(staff);
            })
            .collectList()
            .map(list -> list.get(0).getId());
    }

    private Mono<Integer> seedVehicles(OrganizationEntity org, AgencyEntity agency, int count) {
        return categoryRepository.findAllByOrganizationIdOrSystem(org.getId())
            .next()
            .switchIfEmpty(Mono.defer(() -> {
                VehicleCategoryEntity defaultCat = VehicleCategoryEntity.builder()
                    .id(UUID.randomUUID())
                    .organizationId(org.getId())
                    .name("Default Category " + UUID.randomUUID().toString().substring(0, 5))
                    .description("Created by Seeder")
                    .isNewRecord(true)
                    .build();
                return categoryRepository.save(defaultCat);
            }))
            .flatMap(category -> {
                return Flux.range(1, count)
                    .flatMap(i -> {
                        String plate = "LT-" + org.getName().substring(0,2).toUpperCase() + "-" + agency.getId().toString().substring(0,2) + i;

                        VehicleEntity vehicle = VehicleEntity.builder()
                            .id(UUID.randomUUID())
                            .organizationId(org.getId())
                            .agencyId(agency.getId())
                            .categoryId(category.getId())
                            .licencePlate(plate)
                            .brand(i % 2 == 0 ? "Toyota" : "Hyundai")
                            .model(i % 2 == 0 ? "Corolla" : "Tucson")
                            .yearProduction(LocalDateTime.now().minusYears(i))
                            .places(5)
                            .kilometrage(10000.0 * i)
                            .statut("AVAILABLE")
                            .transmission("AUTOMATIC")
                            .color("White")
                            .imagesList(createJson(new String[]{"https://placehold.co/600x400?text=Voiture+" + plate}))
                            .descriptionList(createJson(new String[]{"Climatisation", "Bluetooth", "GPS"}))
                            .functionalities(createJson(Map.of("gps", true, "ac", true)))
                            .engineDetails(createJson(Map.of("type", "V4", "horsepower", 120)))
                            .fuelEfficiency(createJson(Map.of("city", "8L/100", "highway", "6L/100")))
                            .insuranceDetails(createJson(Map.of("provider", "AXA", "expiry", "2026-01-01")))
                            .createdAt(LocalDateTime.now())
                            .isNewRecord(true)
                            .build();

                        return vehicleRepository.save(vehicle);
                    })
                    .count()
                    .map(Long::intValue);
            });
    }

    private Mono<Integer> seedDrivers(OrganizationEntity org, AgencyEntity agency, int count) {
        return Flux.range(1, count)
            .flatMap(i -> {
                DriverEntity driver = DriverEntity.builder()
                    .id(UUID.randomUUID())
                    .organizationId(org.getId())
                    .agencyId(agency.getId())
                    .firstname("Chauffeur")
                    .lastname(agency.getName() + " " + i)
                    .tel("+2376900000" + i)
                    .age(25 + i)
                    .gender(0)
                    .profilUrl("https://placehold.co/150x150?text=Profil+" + i)
                    .cniUrl("https://placehold.co/400x300?text=CNI+" + i)
                    .drivingLicenseUrl("https://placehold.co/400x300?text=Permis+" + i)
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isNewRecord(true)
                    .build();

                return driverRepository.save(driver);
            })
            .count()
            .map(Long::intValue);
    }

    private Mono<Void> updateOrganizationCounters(UUID orgId, int agencyCount, SubscriptionPlanEntity plan) {
        return organizationRepository.findById(orgId)
            .flatMap(org -> {
                Mono<Long> totalVehicles = vehicleRepository.findAllByOrganizationId(orgId).count();
                Mono<Long> totalDrivers = driverRepository.findAllByOrganizationId(orgId).count();
                Mono<Long> totalStaff = staffRepository.findAllStaffByOrganizationId(orgId).count();

                return Mono.zip(totalVehicles, totalDrivers, totalStaff)
                    .flatMap(tuple -> {
                        org.setCurrentAgencies(agencyCount);
                        org.setCurrentVehicles(tuple.getT1().intValue());
                        org.setCurrentDrivers(tuple.getT2().intValue());
                        org.setCurrentUsers(tuple.getT3().intValue());
                        return organizationRepository.save(org);
                    });
            })
            .then();
    }

    private Json createJson(Object object) {
        try {
            return Json.of(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            log.error("Erreur JSON", e);
            return Json.of("{}");
        }
    }
}
