package com.asusoftware.Employee_Management_API.model.dto;

import jakarta.validation.constraints.*;
import java.util.*;


public record RegisterCompanyRequest(
        @NotBlank String companyName,
        @Pattern(regexp = "^[a-z0-9-]{3,}$") String slug,
        @Email String ownerEmail,
        @NotBlank String ownerFirstName,
        @NotBlank String ownerLastName,
        @Size(min=8) String password
) {}