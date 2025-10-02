package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.AttendanceStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceUpsertRequest(UUID userId, @NotNull LocalDate date, @NotNull AttendanceStatus status, String notes) {}
