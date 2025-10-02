package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.LeaveStatus;
import com.asusoftware.Employee_Management_API.model.LeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveDto(UUID id, UUID userId, LeaveType type, LocalDate startDate, LocalDate endDate, BigDecimal days, LeaveStatus status, UUID approverId, String reason) {}
