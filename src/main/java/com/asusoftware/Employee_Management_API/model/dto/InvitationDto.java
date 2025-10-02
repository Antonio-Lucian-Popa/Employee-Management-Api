package com.asusoftware.Employee_Management_API.model.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitationDto(UUID id, String email, String role, String token, OffsetDateTime expiresAt, OffsetDateTime acceptedAt) {}