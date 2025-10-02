package com.asusoftware.Employee_Management_API.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "app_user",
        uniqueConstraints = @UniqueConstraint(name = "uq_app_user_tenant_email", columnNames = {"tenant_id","email"}),
        indexes = {
                @Index(name = "idx_app_user_tenant", columnList = "tenant_id"),
                @Index(name = "idx_app_user_created_at", columnList = "created_at")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id
    @GeneratedValue
    private UUID id;


    @Column(name = "tenant_id", nullable = false)
    private String tenantId;


    @Column(nullable = false)
    private String email;


    private String password; // nullable for social


    @Column(name = "first_name", nullable = false)
    private String firstName;


    @Column(name = "last_name", nullable = false)
    private String lastName;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
