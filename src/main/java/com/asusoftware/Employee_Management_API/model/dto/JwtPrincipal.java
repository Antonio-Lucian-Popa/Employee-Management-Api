package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.Role;

import java.security.Principal;
import java.util.UUID;

/**
 * Informația minimă pe care o propagăm în SecurityContext
 * după validarea JWT-ului.
 */
public record JwtPrincipal(UUID userId, String tenant, Role role) implements Principal {
    @Override
    public String getName() {
        return userId != null ? userId.toString() : "anonymous";
    }
}
