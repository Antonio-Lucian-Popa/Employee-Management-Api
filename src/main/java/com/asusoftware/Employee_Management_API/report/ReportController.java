package com.asusoftware.Employee_Management_API.report;

import com.asusoftware.Employee_Management_API.model.dto.MonthlyReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService svc;

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN') and @planAccess.hasAnyPlan('PRO','ENTERPRISE')")
    public MonthlyReportResponse monthly(@RequestParam String month){
        return svc.monthly(YearMonth.parse(month));
    }
}
