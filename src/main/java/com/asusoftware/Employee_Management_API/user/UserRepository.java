package com.asusoftware.Employee_Management_API.user;

import com.asusoftware.Employee_Management_API.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByTenantIdAndEmailIgnoreCase(String tenantId, String email);
    List<AppUser> findAllByTenantId(String tenantId);
    long countByTenantId(String tenantId);
}
