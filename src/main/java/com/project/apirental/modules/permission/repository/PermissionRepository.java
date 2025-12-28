package com.project.apirental.modules.permission.repository;

import com.project.apirental.modules.permission.domain.PermissionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import java.util.UUID;

public interface PermissionRepository extends R2dbcRepository<PermissionEntity, UUID> {
}
