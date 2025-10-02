package com.asusoftware.Employee_Management_API.auditlog;

import com.asusoftware.Employee_Management_API.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> { }