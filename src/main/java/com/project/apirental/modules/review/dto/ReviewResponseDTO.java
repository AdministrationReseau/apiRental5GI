package com.project.apirental.modules.review.dto;

import com.project.apirental.shared.enums.ResourceType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponseDTO(
    UUID id,
    UUID resourceId,
    ResourceType resourceType,
    Integer rating,
    String comment,
    String authorName,
    LocalDateTime createdAt
) {}
