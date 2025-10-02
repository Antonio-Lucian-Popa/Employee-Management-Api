package com.asusoftware.Employee_Management_API.model;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDate;
import java.util.UUID;


@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uq_att_unique_day", columnNames = {"tenant_id","user_id","date"}),
        indexes = {
                @Index(name = "idx_att_tenant_date", columnList = "tenant_id,date"),
                @Index(name = "idx_att_user_date", columnList = "user_id,date")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {
    @Id @GeneratedValue
    private UUID id;


    @Column(name = "tenant_id", nullable = false)
    private String tenantId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_att_user"))
    private AppUser user;


    @Column(nullable = false)
    private LocalDate date;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;


    private String notes;
}
