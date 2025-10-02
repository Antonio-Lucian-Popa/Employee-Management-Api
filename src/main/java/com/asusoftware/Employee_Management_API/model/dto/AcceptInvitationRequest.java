package com.asusoftware.Employee_Management_API.model.dto;

import jakarta.validation.constraints.*;

public record AcceptInvitationRequest(@NotBlank String firstName, @NotBlank String lastName, @Size(min=8) String password) {}
