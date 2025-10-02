package com.asusoftware.Employee_Management_API.user;

import com.asusoftware.Employee_Management_API.model.Role;
import com.asusoftware.Employee_Management_API.model.UserStatus;

public record UpdateUserRequest(
        Role role,
        UserStatus status,
        String firstName,
        String lastName
) {
}