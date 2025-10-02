package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.Role;
import com.asusoftware.Employee_Management_API.model.UserStatus;

import java.util.UUID;

public record UserDto(UUID id, String email, String firstName, String lastName, Role role, UserStatus status) {}