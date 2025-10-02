package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.SubscriptionPlan;
import jakarta.validation.constraints.*;

public record PlanRequest(@NotNull SubscriptionPlan plan) {}
