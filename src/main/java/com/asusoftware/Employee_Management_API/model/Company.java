package com.asusoftware.Employee_Management_API.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "company")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {
    @Id
    @GeneratedValue
    private UUID id;


    @Column(nullable = false)
    private String name;


    @Column(nullable = false, unique = true)
    private String slug; // tenant identifier


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
