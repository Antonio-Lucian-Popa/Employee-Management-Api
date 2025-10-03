package com.asusoftware.Employee_Management_API.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "invitation",
        indexes = {
                @Index(name = "idx_invitation_tenant_email", columnList = "tenant_id,email"),
                @Index(name = "idx_invitation_expires_at", columnList = "expires_at"),
                @Index(name = "idx_invitation_token", columnList = "token")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invitation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(name = "tenant_id", nullable = false)
    private String tenantId;


    @Column(nullable = false)
    private String email;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;


    @Column(nullable = false, unique = true)
    private String token;


    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;


    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",
            foreignKey = @ForeignKey(name = "fk_invitation_created_by"))
    private AppUser createdBy;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
