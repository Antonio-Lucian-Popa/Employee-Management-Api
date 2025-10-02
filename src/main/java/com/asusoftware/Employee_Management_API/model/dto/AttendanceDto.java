package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.AttendanceStatus;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceDto(UUID id, UUID userId, LocalDate date, AttendanceStatus status, String notes) {}
