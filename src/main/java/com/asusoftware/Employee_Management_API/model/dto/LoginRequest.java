package com.asusoftware.Employee_Management_API.model.dto;

import jakarta.validation.constraints.*;
public record LoginRequest(@Email String email, @NotBlank String password) {}
