package com.asusoftware.Employee_Management_API.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_tenant_time", columnList = "tenant_id,created_at"),
                @Index(name = "idx_audit_entity", columnList = "entity,entity_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue
    private UUID id;


    @Column(name = "tenant_id", nullable = false)
    private String tenantId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id",
            foreignKey = @ForeignKey(name = "fk_audit_actor"))
    private AppUser actorUser;


    @Column(nullable = false)
    private String action;


    @Column(nullable = false)
    private String entity;


    @Column(name = "entity_id", nullable = false)
    private String entityId;


    @Column(columnDefinition = "jsonb")
    private String payload; // stocăm JSON serializat


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
