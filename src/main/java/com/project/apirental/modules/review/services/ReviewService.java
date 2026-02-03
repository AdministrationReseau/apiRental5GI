package com.project.apirental.modules.review.services;

import com.project.apirental.modules.driver.repository.DriverRepository;
import com.project.apirental.modules.review.domain.ReviewEntity;
import com.project.apirental.modules.review.dto.ReviewRequestDTO;
import com.project.apirental.modules.review.dto.ReviewResponseDTO;
import com.project.apirental.modules.review.repository.ReviewRepository;
import com.project.apirental.modules.vehicle.repository.VehicleRepository;
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
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public Mono<ReviewResponseDTO> addReview(ReviewRequestDTO request) {
        ReviewEntity review = ReviewEntity.builder()
                .id(UUID.randomUUID())
                .resourceId(request.resourceId())
                .resourceType(request.resourceType())
                .rating(request.rating())
                .comment(request.comment())
                .authorName(request.authorName() != null ? request.authorName() : "Anonyme")
                .createdAt(LocalDateTime.now())
                .isNewRecord(true)
                .build();

        return reviewRepository.save(review)
                .flatMap(savedReview -> updateResourceRating(request.resourceType(), request.resourceId())
                        .thenReturn(mapToDto(savedReview)));
    }

    public Flux<ReviewResponseDTO> getReviews(ResourceType type, UUID resourceId) {
        return reviewRepository.findAllByResourceTypeAndResourceId(type, resourceId)
                .map(this::mapToDto);
    }

    // Met à jour la note moyenne dans la table parente (Vehicle ou Driver)
    private Mono<Void> updateResourceRating(ResourceType type, UUID resourceId) {
        return reviewRepository.getAverageRating(type, resourceId)
                .defaultIfEmpty(0.0)
                .flatMap(avg -> {
                    if (type == ResourceType.VEHICLE) {
                        return vehicleRepository.findById(resourceId)
                                .flatMap(v -> {
                                    v.setRating(avg);
                                    return vehicleRepository.save(v);
                                }).then();
                    } else if (type == ResourceType.DRIVER) {
                        return driverRepository.findById(resourceId)
                                .flatMap(d -> {
                                    d.setRating(avg);
                                    return driverRepository.save(d);
                                }).then();
                    }
                    return Mono.empty();
                });
    }

    private ReviewResponseDTO mapToDto(ReviewEntity entity) {
        return new ReviewResponseDTO(
                entity.getId(),
                entity.getResourceId(),
                entity.getResourceType(),
                entity.getRating(),
                entity.getComment(),
                entity.getAuthorName(),
                entity.getCreatedAt()
        );
    }
}
