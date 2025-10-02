package com.asusoftware.Employee_Management_API.model.dto;

import jakarta.validation.constraints.*;

public record RefreshRequest(@NotBlank String refreshToken) {}
