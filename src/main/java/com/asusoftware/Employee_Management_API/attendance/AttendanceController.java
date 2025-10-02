package com.asusoftware.Employee_Management_API.attendance;

import com.asusoftware.Employee_Management_API.model.dto.AttendanceDto;
import com.asusoftware.Employee_Management_API.model.dto.AttendanceUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService svc;

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public AttendanceDto upsert(@Valid @RequestBody AttendanceUpsertRequest req){ return svc.upsert(req); }

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<AttendanceDto> daily(@RequestParam LocalDate date){ return svc.daily(date); }
}
