package com.asusoftware.Employee_Management_API.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "leave_request",
        indexes = {
                @Index(name = "idx_leave_tenant_dates", columnList = "tenant_id,start_date,end_date"),
                @Index(name = "idx_leave_user", columnList = "user_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveRequest {
    @Id @GeneratedValue
    private UUID id;


    @Column(name = "tenant_id", nullable = false)
    private String tenantId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_leave_user"))
    private AppUser user;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType type;


    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;


    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;


    @Column(nullable = false, precision = 5, scale = 2)
    private java.math.BigDecimal days;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id",
            foreignKey = @ForeignKey(name = "fk_leave_approver"))
    private AppUser approver;


    private String reason;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;


    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
