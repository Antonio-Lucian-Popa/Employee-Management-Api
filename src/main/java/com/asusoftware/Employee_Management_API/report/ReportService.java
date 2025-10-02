package com.asusoftware.Employee_Management_API.report;

import com.asusoftware.Employee_Management_API.attendance.AttendanceRepository;
import com.asusoftware.Employee_Management_API.config.TenantContext;
import com.asusoftware.Employee_Management_API.model.Attendance;
import com.asusoftware.Employee_Management_API.model.AttendanceStatus;
import com.asusoftware.Employee_Management_API.model.dto.MonthlyReportResponse;
import com.asusoftware.Employee_Management_API.model.dto.MonthlyReportRow;
import com.asusoftware.Employee_Management_API.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AttendanceRepository attendance;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public MonthlyReportResponse monthly(YearMonth month){
        String tenant = TenantContext.getTenant();

        // Inițializează rânduri pentru fiecare user din tenant
        Map<UUID, MonthlyReportRow> rows = new LinkedHashMap<>();
        users.findAllByTenantId(tenant).forEach(u ->
                rows.put(u.getId(), new MonthlyReportRow(u.getFirstName() + " " + u.getLastName(), 0, 0)));

        // Parcurge fiecare zi din lună (simplu pentru MVP)
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<Attendance> daily = attendance.findByTenantIdAndDate(tenant, d);
            for (Attendance a : daily) {
                MonthlyReportRow prev = rows.get(a.getUser().getId());
                if (prev == null) continue;
                long present = prev.presentDays();
                long off = prev.offDays();
                if (a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.REMOTE) {
                    present += 1;
                } else if (a.getStatus() == AttendanceStatus.LEAVE || a.getStatus() == AttendanceStatus.SICK || a.getStatus() == AttendanceStatus.OFF) {
                    off += 1;
                }
                rows.put(a.getUser().getId(), new MonthlyReportRow(prev.userFullName(), present, off));
            }
        }

        return new MonthlyReportResponse(month, new ArrayList<>(rows.values()));
    }
}