package com.asusoftware.Employee_Management_API.attendance;

import com.asusoftware.Employee_Management_API.model.dto.AttendanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService service;


    @GetMapping("/daily")
    public List<AttendanceDto> daily(@RequestParam LocalDate date) {
        return service.dailyForTenant(date);
    }
}
