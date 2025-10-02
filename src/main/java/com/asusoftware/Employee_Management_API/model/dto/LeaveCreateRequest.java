package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.LeaveType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveCreateRequest(@NotNull UUID userId, @NotNull LeaveType type, @NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotNull BigDecimal days, String reason) {}
