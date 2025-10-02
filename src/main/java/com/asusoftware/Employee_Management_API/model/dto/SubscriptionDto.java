package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.SubscriptionPlan;
import com.asusoftware.Employee_Management_API.model.SubscriptionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionDto(UUID companyId, SubscriptionPlan plan, SubscriptionStatus status, OffsetDateTime trialUntil, OffsetDateTime nextPaymentDue) {}
