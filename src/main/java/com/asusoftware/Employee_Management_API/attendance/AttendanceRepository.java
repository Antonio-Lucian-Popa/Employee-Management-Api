package com.asusoftware.Employee_Management_API.attendance;

import com.asusoftware.Employee_Management_API.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByTenantIdAndUser_IdAndDate(String tenantId, UUID userId, LocalDate date);
    List<Attendance> findByTenantIdAndDate(String tenantId, LocalDate date);
}
