package com.asusoftware.Employee_Management_API.model;

import jakarta.persistence.*;
import lombok.*;


import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "subscription",
        uniqueConstraints = @UniqueConstraint(name = "uq_subscription_company", columnNames = {"company_id"}),
        indexes = @Index(name = "idx_subscription_next_due", columnList = "next_payment_due")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {
    @Id @GeneratedValue
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sub_company"))
    private Company company;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;


    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;


    @Column(name = "trial_until")
    private OffsetDateTime trialUntil;


    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;


    @Column(name = "last_payment_at")
    private OffsetDateTime lastPaymentAt;


    @Column(name = "next_payment_due")
    private OffsetDateTime nextPaymentDue;


    @Column(name = "external_customer_id")
    private String externalCustomerId;


    @Column(name = "external_sub_id")
    private String externalSubId;
}
