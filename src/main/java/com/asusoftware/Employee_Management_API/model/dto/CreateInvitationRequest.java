package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.Role;
import jakarta.validation.constraints.*;

public record CreateInvitationRequest(@Email String email, Role role) {}
