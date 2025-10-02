package com.asusoftware.Employee_Management_API.leave;

import com.asusoftware.Employee_Management_API.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByTenantIdAndUser_Id(String tenantId, UUID userId);
    List<LeaveRequest> findByTenantIdAndStartDateBetween(String tenantId, LocalDate from, LocalDate to);
}
