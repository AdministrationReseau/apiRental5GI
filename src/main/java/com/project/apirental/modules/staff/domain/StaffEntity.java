package com.project.apirental.modules.staff.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("staff")
public class StaffEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    private UUID userId;
    private UUID organizationId;
    private UUID agencyId;
    private UUID posteId;
    
    @Builder.Default
    private String status = "ACTIVE";
    private LocalDateTime hiredAt;

    @Transient
    @Builder.Default
    @JsonIgnore
    private boolean isNewRecord = false;

    @Override
    @Transient
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}